package com.itmonteur.hospitalerp.appointments;

import java.util.Optional;
import com.itmonteur.hospitalerp.appointments.internal.AppointmentMapper;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ForbiddenException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.appointments.internal.AppointmentRepository;
import com.itmonteur.hospitalerp.common.Gender;
import com.itmonteur.hospitalerp.notifications.NotificationService;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import com.itmonteur.hospitalerp.patients.PtRelative;
import com.itmonteur.hospitalerp.patients.PtRelativeService;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.DoctorService;
import com.itmonteur.hospitalerp.scheduling.Slot;
import com.itmonteur.hospitalerp.scheduling.SlotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;

@Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final PtInfoService ptInfoService;
    private final PtRelativeService ptRelativeService;
    private final DoctorService doctorService;
    private final SlotService slotService;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserService currentUserService;

    public AppointmentService(AppointmentRepository appointmentRepository, PtInfoService ptInfoService,
                              PtRelativeService ptRelativeService, DoctorService doctorService,
                              SlotService slotService, ApplicationEventPublisher eventPublisher,
                              CurrentUserService currentUserService) {
        this.appointmentRepository = appointmentRepository;
        this.ptInfoService = ptInfoService;
        this.ptRelativeService = ptRelativeService;
        this.doctorService = doctorService;
        this.slotService = slotService;
        this.eventPublisher = eventPublisher;
        this.currentUserService = currentUserService;
    }

    // ---------------------------------------------------------------- queries

    public List<AppointmentDTO> getAllAppointments() {
        return toDTOs(appointmentRepository.findAll());
    }

    /** All appointments of the logged-in patient (including cancelled ones, for history). */
    public List<AppointmentDTO> getMyAppointments() {
        PtInfo patient = currentPatient();
        return toDTOs(appointmentRepository.findByPtInfo_PatientId(patient.getPatientId()));
    }

    public AppointmentDTO getAppointmentByID(long appointmentID) {
        Appointment appointment = findAppointment(appointmentID);
        requireCanView(appointment);
        return AppointmentMapper.toDTO(appointment);
    }

    public List<AppointmentDTO> getAllPatientCompletedAppointments(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN, Role.RECEPTIONIST);
        PtInfo patient = patientByUserId(userId);
        return toDTOs(appointmentRepository.findCompletedByPatientId(patient.getPatientId()));
    }

    public List<AppointmentDTO> getAllPatientPendingAppointments(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN, Role.RECEPTIONIST);
        PtInfo patient = patientByUserId(userId);
        return toDTOs(appointmentRepository.findPendingByPatientId(patient.getPatientId()));
    }

    /** All appointments of the logged-in doctor. */
    public List<AppointmentDTO> getAppointmentsForDoctor() {
        Doctor doctor = doctorByUserId(currentUserService.getCurrentUserId());
        return toDTOs(appointmentRepository.findByDoctor_Id(doctor.getId()));
    }

    public List<AppointmentDTO> getAllPendingAppointments() {
        return toDTOs(appointmentRepository.findAllPending());
    }

    public List<AppointmentDTO> getAllCompletedAppointments() {
        return toDTOs(appointmentRepository.findAllCompleted());
    }

    public List<AppointmentDTO> getAppointmentsByDrName(String doctorName) {
        return toDTOs(appointmentRepository.findByDoctor_Name(doctorName));
    }

    // ---------------------------------------------------------------- commands

    /**
     * Books a slot. Patients always book for themselves (or one of their relatives);
     * the ptInfoId in the request is only honoured for staff (admin/receptionist).
     */
    @Transactional
    public AppointmentDTO createAppointment(AppointmentDTO dto) {
        if (dto.getSlotId() == null) {
            throw new BadRequestException("Please select a slot");
        }
        PtInfo ptInfo;
        if (currentUserService.isStaff()) {
            if (dto.getPtInfoId() == null) {
                throw new BadRequestException("ptInfoId is required when booking on behalf of a patient");
            }
            ptInfo = ptInfoService.findPatientEntity(dto.getPtInfoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", dto.getPtInfoId()));
        } else if (currentUserService.hasRole(Role.PATIENT)) {
            ptInfo = currentPatient();
        } else {
            throw new ForbiddenException("Only patients and hospital staff can book appointments");
        }

        Appointment appointment = new Appointment();
        appointment.setPtInfo(ptInfo);
        appointment.setMessage(dto.getMessage());
        applyPatientDetails(appointment, ptInfo, dto);

        Slot slot = slotService.lockAndBook(dto.getSlotId());
        appointment.setSlot(slot);
        appointment.setDoctor(slot.getDoctor());
        appointment.setShift(slot.getShift());
        appointment.setDate(slot.getDate());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        Appointment saved = appointmentRepository.save(appointment);
        logger.info("Appointment {} booked for slot {}", saved.getAppointmentID(), slot.getId());
        // Sent after commit by AppointmentNotificationListener
        eventPublisher.publishEvent(new AppointmentNotificationEvent(
                AppointmentNotificationEvent.Kind.BOOKED, notificationInfo(saved)));
        return AppointmentMapper.toDTO(saved);
    }

    /** Soft-cancels: the record is kept for history and the slot becomes bookable again. */
    @Transactional
    public AppointmentDTO cancelAppointment(long appointmentID) {
        Appointment appointment = findAppointment(appointmentID);
        requireCanModify(appointment);
        if (!appointment.isActive()) {
            throw new BadRequestException("Only upcoming appointments can be cancelled");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED_BY_PATIENT);
        slotService.releaseSlot(appointment.getSlot());
        Appointment saved = appointmentRepository.save(appointment);
        logger.info("Appointment {} cancelled", appointmentID);
        eventPublisher.publishEvent(new AppointmentNotificationEvent(
                AppointmentNotificationEvent.Kind.CANCELLED, notificationInfo(saved)));
        return AppointmentMapper.toDTO(saved);
    }

    /** Reschedules (when slotId changes) and/or updates the note and patient details. */
    @Transactional
    public AppointmentDTO updateAppointmentById(long appointmentID, AppointmentDTO dto) {
        Appointment appointment = findAppointment(appointmentID);
        requireCanModify(appointment);
        if (!appointment.isActive()) {
            throw new BadRequestException("Cannot update a cancelled or completed appointment. Please create a new one.");
        }
        if (dto.getPatientName() != null && !dto.getPatientName().isBlank()) {
            appointment.setPatientName(dto.getPatientName());
        }
        if (dto.getGender() != null) {
            appointment.setGender(dto.getGender());
        }
        if (dto.getAge() > 0) {
            appointment.setAge(dto.getAge());
        }
        if (dto.getMessage() != null) {
            appointment.setMessage(dto.getMessage());
        }

        Slot oldSlot = appointment.getSlot();
        if (dto.getSlotId() != null && (oldSlot == null || !Objects.equals(oldSlot.getId(), dto.getSlotId()))) {
            // Book the new slot first; the old one is released only once the new one is secured
            Slot newSlot = slotService.lockAndBook(dto.getSlotId());
            slotService.releaseSlot(oldSlot);
            appointment.setSlot(newSlot);
            appointment.setDoctor(newSlot.getDoctor());
            appointment.setShift(newSlot.getShift());
            appointment.setDate(newSlot.getDate());
            appointment.setReminderSent(false); // remind again for the new date
            logger.info("Appointment {} rescheduled to slot {}", appointmentID, newSlot.getId());
        }
        return AppointmentMapper.toDTO(appointmentRepository.save(appointment));
    }

    // ---------------------------------------------------------------- doctor views
    // (moved from DoctorService in step 1.6; same behaviour)

    /** Only the doctor who owns the appointment (or an admin) can complete it. */
    @Transactional
    public String markAsCompleted(long appointmentId) {
        Appointment appointment = findAppointment(appointmentId);
        if (!currentUserService.hasRole(Role.ADMIN)) {
            Long userId = currentUserService.getCurrentUserId();
            boolean ownsAppointment = appointment.getDoctor() != null && appointment.getDoctor().getUser() != null
                    && Objects.equals(appointment.getDoctor().getUser().getId(), userId);
            if (!ownsAppointment) {
                throw new ForbiddenException("You can only complete your own appointments");
            }
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.isCompleted()) {
            return "Appointment is already marked as completed.";
        }
        if (!appointment.isActive()) {
            throw new BadRequestException("A cancelled appointment cannot be completed");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
        return "Appointment marked as completed successfully.";
    }

    /** userId = the doctor's user id; allowed for the doctor, admins and receptionists. */
    public List<AppointmentDTO> getPendingAppointmentsForDoctorUser(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN, Role.RECEPTIONIST);
        return toDTOs(appointmentRepository.findPendingByDoctorId(doctorByUserId(userId).getId()));
    }

    public List<AppointmentDTO> getCompletedAppointmentsForDoctorUser(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN, Role.RECEPTIONIST);
        return toDTOs(appointmentRepository.findCompletedByDoctorId(doctorByUserId(userId).getId()));
    }

    /** doctorId = doctor.id (not the user id). */
    public long countPendingForDoctor(Long doctorId) {
        return appointmentRepository.countPendingByDoctorId(doctorId);
    }

    public long countCompletedForDoctor(Long doctorId) {
        return appointmentRepository.countCompletedByDoctorId(doctorId);
    }

    // ---------------------------------------------------------------- module API
    // (used by clinical and administration instead of AppointmentRepository)

    public Optional<Appointment> findAppointmentEntity(long appointmentId) {
        return appointmentRepository.findById(appointmentId);
    }

    /** Called by the clinical module when a consultation is saved for an upcoming appointment. */
    @Transactional
    public void markCompletedByConsultation(Appointment appointment) {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
    }

    /** Whether the doctor has (or had) an appointment with the patient. */
    public boolean hasAppointment(Long doctorId, Long patientId) {
        return appointmentRepository.existsByDoctor_IdAndPtInfo_PatientId(doctorId, patientId);
    }

    public List<Appointment> findUpcomingForPatient(Long patientId) {
        return appointmentRepository.findPendingByPatientId(patientId);
    }

    public List<Appointment> findUpcomingForDoctor(Long doctorId) {
        return appointmentRepository.findPendingByDoctorId(doctorId);
    }

    /** Account deletion: removes all of a patient's appointments (consultations must be removed first). */
    @Transactional
    public void deleteAllForPatient(Long patientId) {
        appointmentRepository.deleteByPatientId(patientId);
    }

    /** Account deletion: removes all of a doctor's appointments (consultations must be removed first). */
    @Transactional
    public void deleteAllForDoctor(Long doctorId) {
        appointmentRepository.deleteByDoctorId(doctorId);
    }

    // ---------------------------------------------------------------- helpers

    private void applyPatientDetails(Appointment appointment, PtInfo ptInfo, AppointmentDTO dto) {
        if (dto.getRelativeId() != null) {
            PtRelative relative = ptRelativeService.findRelativeEntity(dto.getRelativeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Relative", "id", dto.getRelativeId()));
            if (relative.getPtInfo() == null
                    || !Objects.equals(relative.getPtInfo().getPatientId(), ptInfo.getPatientId())) {
                throw new ForbiddenException("This relative is not linked to the patient account");
            }
            appointment.setRelative(relative);
            appointment.setPatientName(relative.getName());
            appointment.setGender(relative.getGender() != null ? relative.getGender() : Gender.OTHER);
            appointment.setAge(ageFrom(relative.getDob(), dto.getAge()));
            return;
        }
        appointment.setPatientName(dto.getPatientName() != null && !dto.getPatientName().isBlank()
                ? dto.getPatientName() : ptInfo.getPatientName());
        Gender gender = dto.getGender() != null ? dto.getGender() : ptInfo.getGender();
        appointment.setGender(gender != null ? gender : Gender.OTHER);
        appointment.setAge(dto.getAge() > 0 ? dto.getAge() : ageFrom(ptInfo.getDob(), 0));
    }

    private static int ageFrom(LocalDate dob, int fallback) {
        if (dob == null || dob.isAfter(LocalDate.now()) || dob.getYear() < 1900) {
            return fallback;
        }
        return Period.between(dob, LocalDate.now()).getYears();
    }

    private void requireCanView(Appointment appointment) {
        if (currentUserService.isStaff()) {
            return;
        }
        Long userId = currentUserService.getCurrentUserId();
        boolean isPatient = appointment.getPtInfo() != null && appointment.getPtInfo().getUser() != null
                && Objects.equals(appointment.getPtInfo().getUser().getId(), userId);
        boolean isDoctor = appointment.getDoctor() != null && appointment.getDoctor().getUser() != null
                && Objects.equals(appointment.getDoctor().getUser().getId(), userId);
        if (!isPatient && !isDoctor) {
            throw new ForbiddenException("You can only view your own appointments");
        }
    }

    private void requireCanModify(Appointment appointment) {
        if (currentUserService.isStaff()) {
            return;
        }
        Long userId = currentUserService.getCurrentUserId();
        boolean isPatient = appointment.getPtInfo() != null && appointment.getPtInfo().getUser() != null
                && Objects.equals(appointment.getPtInfo().getUser().getId(), userId);
        if (!isPatient) {
            throw new ForbiddenException("You can only change your own appointments");
        }
    }

    private Doctor doctorByUserId(Long userId) {
        return doctorService.findDoctorEntityByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", userId));
    }

    private PtInfo currentPatient() {
        return patientByUserId(currentUserService.getCurrentUserId());
    }

    private PtInfo patientByUserId(Long userId) {
        return ptInfoService.findPatientEntityByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
    }

    private Appointment findAppointment(long appointmentID) {
        return appointmentRepository.findById(appointmentID)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentID));
    }

    private static List<AppointmentDTO> toDTOs(List<Appointment> appointments) {
        return appointments.stream().map(AppointmentMapper::toDTO).toList();
    }

    public static NotificationService.AppointmentInfo notificationInfo(Appointment appointment) {
        PtInfo patient = appointment.getPtInfo();
        Doctor doctor = appointment.getDoctor();
        Slot slot = appointment.getSlot();
        return new NotificationService.AppointmentInfo(
                appointment.getPatientName(),
                patient != null ? patient.getEmail() : null,
                patient != null ? patient.getContactNo() : null,
                doctor != null ? doctor.getName() : "",
                doctor != null ? doctor.getEmail() : null,
                doctor != null ? doctor.getPhoneNumber() : null,
                String.valueOf(appointment.getDate()),
                slot != null ? slot.getStartTime() + " - " + slot.getEndTime() : "");
    }
}
