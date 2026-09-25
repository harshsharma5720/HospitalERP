package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.ConsultationDTO;
import ITmonteur.example.hospitalERP.dto.PrescriptionItemDTO;
import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ForbiddenException;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.repositories.ConsultationRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Consultation notes and prescriptions.
 * Medical records are visible only to the patient, the treating doctor(s) and admins —
 * receptionists can manage appointments but cannot read clinical notes.
 */
@Service
public class ConsultationService {

    private static final Logger logger = LoggerFactory.getLogger(ConsultationService.class);

    private final ConsultationRepository consultationRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PtInfoRepository ptInfoRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public ConsultationService(ConsultationRepository consultationRepository, AppointmentRepository appointmentRepository,
                               DoctorRepository doctorRepository, PtInfoRepository ptInfoRepository,
                               CurrentUserService currentUserService, Clock clock) {
        this.consultationRepository = consultationRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.ptInfoRepository = ptInfoRepository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    /**
     * Creates or updates the consultation of an appointment. Only the appointment's own doctor can
     * write it. Saving a consultation for an upcoming appointment marks it COMPLETED.
     */
    @Transactional
    public ConsultationDTO saveConsultation(long appointmentId, ConsultationDTO dto) {
        Appointment appointment = findAppointment(appointmentId);
        requireTreatingDoctor(appointment);
        if (!appointment.isActive() && appointment.getStatus() != AppointmentStatus.COMPLETED && !appointment.isCompleted()) {
            throw new BadRequestException("A cancelled appointment cannot have a consultation");
        }
        if (dto.getFollowUpDate() != null && !dto.getFollowUpDate().isAfter(appointment.getDate())) {
            throw new BadRequestException("Follow-up date must be after the appointment date");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Consultation consultation = consultationRepository.findByAppointment_AppointmentID(appointmentId)
                .orElseGet(() -> {
                    Consultation created = new Consultation();
                    created.setAppointment(appointment);
                    created.setCreatedAt(now);
                    return created;
                });
        consultation.setSymptoms(trimToNull(dto.getSymptoms()));
        consultation.setDiagnosis(dto.getDiagnosis().trim());
        consultation.setNotes(trimToNull(dto.getNotes()));
        consultation.setBloodPressure(trimToNull(dto.getBloodPressure()));
        consultation.setPulse(dto.getPulse());
        consultation.setTemperature(dto.getTemperature());
        consultation.setWeightKg(dto.getWeightKg());
        consultation.setFollowUpDate(dto.getFollowUpDate());
        consultation.setUpdatedAt(now);
        consultation.replaceMedicines(dto.getMedicines().stream().map(ConsultationService::toItem).toList());

        if (appointment.isActive()) {
            appointment.setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(appointment);
        }
        Consultation saved = consultationRepository.save(consultation);
        logger.info("Consultation saved for appointment {} ({} medicines)", appointmentId, saved.getMedicines().size());
        return toDTO(saved);
    }

    public ConsultationDTO getByAppointment(long appointmentId) {
        Consultation consultation = findByAppointment(appointmentId);
        requireCanRead(consultation.getAppointment());
        return toDTO(consultation);
    }

    /** Entity access for the PDF generator, with the same read check. */
    @Transactional(readOnly = true)
    public Consultation getForPrescription(long appointmentId) {
        Consultation consultation = findByAppointment(appointmentId);
        requireCanRead(consultation.getAppointment());
        consultation.getMedicines().size(); // initialise while the session is open
        return consultation;
    }

    /** The logged-in patient's own medical history. */
    public List<ConsultationDTO> getMyHistory() {
        Long userId = currentUserService.getCurrentUserId();
        PtInfo patient = ptInfoRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
        return consultationRepository.findHistoryByPatientId(patient.getPatientId()).stream().map(this::toDTO).toList();
    }

    /** A patient's history for a doctor who has an appointment with them (or an admin). */
    public List<ConsultationDTO> getPatientHistory(Long patientId) {
        if (!currentUserService.hasRole(Role.ADMIN)) {
            Doctor doctor = currentDoctor();
            if (!appointmentRepository.existsByDoctor_IdAndPtInfo_PatientId(doctor.getId(), patientId)) {
                throw new ForbiddenException("You can only view the history of your own patients");
            }
        }
        return consultationRepository.findHistoryByPatientId(patientId).stream().map(this::toDTO).toList();
    }

    // ------------------------------------------------------------------ access rules

    private void requireTreatingDoctor(Appointment appointment) {
        Doctor doctor = currentDoctor();
        if (appointment.getDoctor() == null || !Objects.equals(appointment.getDoctor().getId(), doctor.getId())) {
            throw new ForbiddenException("Only the appointment's doctor can write the consultation");
        }
    }

    private void requireCanRead(Appointment appointment) {
        if (currentUserService.hasRole(Role.ADMIN)) {
            return;
        }
        Long userId = currentUserService.getCurrentUserId();
        boolean isPatient = appointment.getPtInfo() != null && appointment.getPtInfo().getUser() != null
                && Objects.equals(appointment.getPtInfo().getUser().getId(), userId);
        boolean isDoctor = appointment.getDoctor() != null && appointment.getDoctor().getUser() != null
                && Objects.equals(appointment.getDoctor().getUser().getId(), userId);
        if (!isPatient && !isDoctor) {
            throw new ForbiddenException("You are not allowed to view this medical record");
        }
    }

    private Doctor currentDoctor() {
        Long userId = currentUserService.getCurrentUserId();
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Only doctors can do this"));
    }

    // ------------------------------------------------------------------ mapping

    private Appointment findAppointment(long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));
    }

    private Consultation findByAppointment(long appointmentId) {
        return consultationRepository.findByAppointment_AppointmentID(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation", "appointmentId", appointmentId));
    }

    private static PrescriptionItem toItem(PrescriptionItemDTO dto) {
        PrescriptionItem item = new PrescriptionItem();
        item.setMedicineName(dto.getMedicineName().trim());
        item.setDosage(trimToNull(dto.getDosage()));
        item.setFrequency(trimToNull(dto.getFrequency()));
        item.setDuration(trimToNull(dto.getDuration()));
        item.setInstructions(trimToNull(dto.getInstructions()));
        return item;
    }

    ConsultationDTO toDTO(Consultation c) {
        ConsultationDTO dto = new ConsultationDTO();
        dto.setId(c.getId());
        Appointment appointment = c.getAppointment();
        dto.setAppointmentId(appointment.getAppointmentID());
        dto.setAppointmentDate(appointment.getDate());
        dto.setPatientName(appointment.getPatientName());
        if (appointment.getDoctor() != null) {
            dto.setDoctorName(appointment.getDoctor().getName());
            dto.setDoctorSpecialist(appointment.getDoctor().getSpecialist() != null
                    ? appointment.getDoctor().getSpecialist().name() : null);
        }
        dto.setSymptoms(c.getSymptoms());
        dto.setDiagnosis(c.getDiagnosis());
        dto.setNotes(c.getNotes());
        dto.setBloodPressure(c.getBloodPressure());
        dto.setPulse(c.getPulse());
        dto.setTemperature(c.getTemperature());
        dto.setWeightKg(c.getWeightKg());
        dto.setFollowUpDate(c.getFollowUpDate());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        dto.setMedicines(c.getMedicines().stream().map(item -> {
            PrescriptionItemDTO i = new PrescriptionItemDTO();
            i.setMedicineName(item.getMedicineName());
            i.setDosage(item.getDosage());
            i.setFrequency(item.getFrequency());
            i.setDuration(item.getDuration());
            i.setInstructions(item.getInstructions());
            return i;
        }).toList());
        return dto;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
