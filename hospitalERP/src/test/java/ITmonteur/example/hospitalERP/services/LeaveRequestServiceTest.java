package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.LeaveRequestDTO;
import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.LeaveRequestRepository;
import ITmonteur.example.hospitalERP.repositories.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private NotificationService notificationService;
    @Mock private CurrentUserService currentUserService;

    private LeaveRequestService service;
    private User doctorUser;

    @BeforeEach
    void setUp() {
        service = new LeaveRequestService(leaveRequestRepository, doctorRepository, appointmentRepository,
                slotRepository, notificationService, currentUserService);
        doctorUser = new User();
        doctorUser.setId(5L);
        doctorUser.setRole(Role.DOCTOR);
    }

    @Test
    void applyUsesLoggedInUserAndDerivesRole() {
        when(currentUserService.getCurrentUser()).thenReturn(doctorUser);
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequestDTO request = new LeaveRequestDTO();
        request.setUserId(999L);          // ignored
        request.setRole("ROLE_ADMIN");    // ignored
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));
        request.setReason("Conference");

        LeaveRequestDTO result = service.createLeaveRequest(request);

        assertThat(result.getUserId()).isEqualTo(5L);
        assertThat(result.getRole()).isEqualTo("ROLE_DOCTOR");
        assertThat(result.getStatus()).isEqualTo(LeaveStatus.PENDING);
    }

    @Test
    void endBeforeStartIsRejected() {
        when(currentUserService.getCurrentUser()).thenReturn(doctorUser);
        LeaveRequestDTO request = new LeaveRequestDTO();
        request.setStartDate(LocalDate.now().plusDays(3));
        request.setEndDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.createLeaveRequest(request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void approvingDoctorLeaveCancelsOnlyActiveAppointmentsAndBlocksSlots() {
        LeaveRequest leave = new LeaveRequest();
        leave.setUser(doctorUser);
        leave.setStatus(LeaveStatus.PENDING);
        leave.setStartDate(LocalDate.now().plusDays(1));
        leave.setEndDate(LocalDate.now().plusDays(2));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leave));

        Doctor doctor = new Doctor();
        doctor.setId(7L);
        doctor.setUser(doctorUser);
        when(doctorRepository.findByUserId(5L)).thenReturn(Optional.of(doctor));

        Appointment active = appointment(AppointmentStatus.SCHEDULED);
        Appointment alreadyCancelled = appointment(AppointmentStatus.CANCELLED_BY_PATIENT);
        when(appointmentRepository.findByDoctor_IdAndDateBetween(7L, leave.getStartDate(), leave.getEndDate()))
                .thenReturn(List.of(active, alreadyCancelled));
        Slot slot = new Slot(leave.getStartDate(), LocalTime.NOON, LocalTime.NOON.plusMinutes(10), doctor, Shift.MORNING);
        when(slotRepository.findByDoctor_IdAndDateBetween(7L, leave.getStartDate(), leave.getEndDate()))
                .thenReturn(List.of(slot));

        service.updateLeaveStatus(1L, LeaveStatus.APPROVED);

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(active.getStatus()).isEqualTo(AppointmentStatus.CANCELLED_BY_DOCTOR);
        assertThat(active.isActive()).isFalse();
        assertThat(alreadyCancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED_BY_PATIENT);
        assertThat(slot.isAvailable()).isFalse();
        verify(notificationService, times(1)).appointmentCancelledByDoctorLeave(any());
    }

    @Test
    void alreadyDecidedLeaveCannotBeChangedAgain() {
        LeaveRequest leave = new LeaveRequest();
        leave.setUser(doctorUser);
        leave.setStatus(LeaveStatus.REJECTED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leave));

        assertThatThrownBy(() -> service.updateLeaveStatus(1L, LeaveStatus.APPROVED))
                .isInstanceOf(BadRequestException.class);
    }

    private static Appointment appointment(AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setStatus(status);
        appointment.setPatientName("P");
        appointment.setDate(LocalDate.now().plusDays(1));
        return appointment;
    }
}
