package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.dto.EntityMapper;
import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ForbiddenException;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import ITmonteur.example.hospitalERP.repositories.PtRelativeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final PtInfoRepository ptInfoRepository;
    private final PtRelativeRepository ptRelativeRepository;
    private final DoctorRepository doctorRepository;
    private final SlotService slotService;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public AppointmentService(AppointmentRepository appointmentRepository, PtInfoRepository ptInfoRepository,
                              PtRelativeRepository ptRelativeRepository, DoctorRepository doctorRepository,
                              SlotService slotService, NotificationService notificationService,
                              CurrentUserService currentUserService) {
        this.appointmentRepository = appointmentRepository;
        this.ptInfoRepository = ptInfoRepository;
        this.ptRelativeRepository = ptRelativeRepository;
        this.doctorRepository = doctorRepository;
        this.slotService = slotService;
        this.notificationService = notificationService;
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
        return EntityMapper.toAppointmentDTO(appointment);
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
        Long userId = currentUserService.getCurrentUserId();
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", userId));
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
            ptInfo = ptInfoRepository.findById(dto.getPtInfoId())
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
        notificationService.appointmentBooked(notificationInfo(saved));
        return EntityMapper.toAppointmentDTO(saved);
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
        notificationService.appointmentCancelled(notificationInfo(saved));
        return EntityMapper.toAppointmentDTO(saved);
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
            logger.info("Appointment {} rescheduled to slot {}", appointmentID, newSlot.getId());
        }
        return EntityMapper.toAppointmentDTO(appointmentRepository.save(appointment));
    }

    // ---------------------------------------------------------------- helpers

    private void applyPatientDetails(Appointment appointment, PtInfo ptInfo, AppointmentDTO dto) {
        if (dto.getRelativeId() != null) {
            PtRelative relative = ptRelativeRepository.findById(dto.getRelativeId())
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

    private PtInfo currentPatient() {
        return patientByUserId(currentUserService.getCurrentUserId());
    }

    private PtInfo patientByUserId(Long userId) {
        return ptInfoRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
    }

    private Appointment findAppointment(long appointmentID) {
        return appointmentRepository.findById(appointmentID)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentID));
    }

    private static List<AppointmentDTO> toDTOs(List<Appointment> appointments) {
        return appointments.stream().map(EntityMapper::toAppointmentDTO).toList();
    }

    static NotificationService.AppointmentInfo notificationInfo(Appointment appointment) {
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
