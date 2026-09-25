package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.ConsultationDTO;
import ITmonteur.example.hospitalERP.dto.PrescriptionItemDTO;
import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ForbiddenException;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.repositories.ConsultationRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock private ConsultationRepository consultationRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private PtInfoRepository ptInfoRepository;
    @Mock private CurrentUserService currentUserService;

    private ConsultationService service;
    private Doctor doctor;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        service = new ConsultationService(consultationRepository, appointmentRepository, doctorRepository,
                ptInfoRepository, currentUserService, Clock.systemDefaultZone());
        User doctorUser = new User();
        doctorUser.setId(5L);
        doctor = new Doctor();
        doctor.setId(7L);
        doctor.setName("Rao");
        doctor.setUser(doctorUser);

        User patientUser = new User();
        patientUser.setId(100L);
        PtInfo patient = new PtInfo();
        patient.setPatientId(10L);
        patient.setUser(patientUser);

        appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPtInfo(patient);
        appointment.setPatientName("Asha");
        appointment.setDate(LocalDate.now());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        lenient().when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        lenient().when(consultationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ConsultationDTO request() {
        ConsultationDTO dto = new ConsultationDTO();
        dto.setDiagnosis("  Viral fever ");
        dto.setBloodPressure("120/80");
        PrescriptionItemDTO med = new PrescriptionItemDTO();
        med.setMedicineName("Paracetamol");
        med.setDosage("500 mg");
        dto.setMedicines(List.of(med));
        return dto;
    }

    @Test
    void treatingDoctorSavesConsultationAndCompletesAppointment() {
        when(currentUserService.getCurrentUserId()).thenReturn(5L);
        when(doctorRepository.findByUserId(5L)).thenReturn(Optional.of(doctor));
        when(consultationRepository.findByAppointment_AppointmentID(1L)).thenReturn(Optional.empty());

        ConsultationDTO saved = service.saveConsultation(1L, request());

        assertThat(saved.getDiagnosis()).isEqualTo("Viral fever");
        assertThat(saved.getMedicines()).extracting(PrescriptionItemDTO::getMedicineName).containsExactly("Paracetamol");
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.isCompleted()).isTrue();
    }

    @Test
    void anotherDoctorCannotWriteTheConsultation() {
        Doctor other = new Doctor();
        other.setId(8L);
        when(currentUserService.getCurrentUserId()).thenReturn(6L);
        when(doctorRepository.findByUserId(6L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.saveConsultation(1L, request())).isInstanceOf(ForbiddenException.class);
        verify(consultationRepository, never()).save(any());
    }

    @Test
    void cancelledAppointmentCannotGetAConsultation() {
        appointment.setStatus(AppointmentStatus.CANCELLED_BY_PATIENT);
        when(currentUserService.getCurrentUserId()).thenReturn(5L);
        when(doctorRepository.findByUserId(5L)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> service.saveConsultation(1L, request())).isInstanceOf(BadRequestException.class);
    }

    @Test
    void followUpMustBeAfterTheVisit() {
        when(currentUserService.getCurrentUserId()).thenReturn(5L);
        when(doctorRepository.findByUserId(5L)).thenReturn(Optional.of(doctor));
        ConsultationDTO dto = request();
        dto.setFollowUpDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> service.saveConsultation(1L, dto)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void otherPatientCannotReadTheRecord() {
        Consultation consultation = new Consultation();
        consultation.setAppointment(appointment);
        consultation.setDiagnosis("x");
        when(consultationRepository.findByAppointment_AppointmentID(1L)).thenReturn(Optional.of(consultation));
        when(currentUserService.hasRole(Role.ADMIN)).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(999L);

        assertThatThrownBy(() -> service.getByAppointment(1L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void doctorWithoutAppointmentCannotSeePatientHistory() {
        when(currentUserService.hasRole(Role.ADMIN)).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(5L);
        when(doctorRepository.findByUserId(5L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctor_IdAndPtInfo_PatientId(7L, 42L)).thenReturn(false);

        assertThatThrownBy(() -> service.getPatientHistory(42L)).isInstanceOf(ForbiddenException.class);
    }
}
