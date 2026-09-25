package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.LeaveRequestDTO;
import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.events.DoctorLeaveApprovedEvent;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private LeaveRequestService service;
    private User doctorUser;

    @BeforeEach
    void setUp() {
        service = new LeaveRequestService(leaveRequestRepository, doctorRepository, currentUserService, eventPublisher);
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
    void approvingDoctorLeavePublishesTheEventWithDoctorIdAndDates() {
        LeaveRequest leave = pendingLeave(doctorUser);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leave));
        Doctor doctor = new Doctor();
        doctor.setId(7L);
        doctor.setUser(doctorUser);
        when(doctorRepository.findByUserId(5L)).thenReturn(Optional.of(doctor));

        service.updateLeaveStatus(1L, LeaveStatus.APPROVED);

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        verify(eventPublisher).publishEvent(
                new DoctorLeaveApprovedEvent(7L, leave.getStartDate(), leave.getEndDate()));
    }

    @Test
    void rejectingOrNonDoctorLeavePublishesNothing() {
        LeaveRequest rejected = pendingLeave(doctorUser);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(rejected));
        service.updateLeaveStatus(1L, LeaveStatus.REJECTED);

        User receptionist = new User();
        receptionist.setId(6L);
        receptionist.setRole(Role.RECEPTIONIST);
        LeaveRequest receptionistLeave = pendingLeave(receptionist);
        when(leaveRequestRepository.findById(2L)).thenReturn(Optional.of(receptionistLeave));
        service.updateLeaveStatus(2L, LeaveStatus.APPROVED);

        verifyNoInteractions(eventPublisher);
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

    private static LeaveRequest pendingLeave(User user) {
        LeaveRequest leave = new LeaveRequest();
        leave.setUser(user);
        leave.setStatus(LeaveStatus.PENDING);
        leave.setStartDate(LocalDate.now().plusDays(1));
        leave.setEndDate(LocalDate.now().plusDays(2));
        return leave;
    }
}
