package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.LeaveRequestDTO;
import ITmonteur.example.hospitalERP.entities.*;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ConflictException;
import com.itmonteur.hospitalerp.common.ForbiddenException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.events.DoctorLeaveApprovedEvent;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.LeaveRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class LeaveRequestService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveRequestService.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final DoctorRepository doctorRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, DoctorRepository doctorRepository,
                               CurrentUserService currentUserService, ApplicationEventPublisher eventPublisher) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.doctorRepository = doctorRepository;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }

    /** Whether the user has an APPROVED leave covering the date. Used by the scheduling module. */
    public boolean isOnApprovedLeave(Long userId, LocalDate date) {
        return leaveRequestRepository.isOnApprovedLeave(userId, date);
    }

    /** Applies for leave as the logged-in user (the userId/role in the request are ignored). */
    public LeaveRequestDTO createLeaveRequest(LeaveRequestDTO dto) {
        User user = currentUserService.getCurrentUser();
        validateDates(dto.getStartDate(), dto.getEndDate());
        if (leaveRequestRepository.existsOverlapping(user.getId(), dto.getStartDate(), dto.getEndDate(), null)) {
            throw new ConflictException("Leave Request already exists for these dates! Choose different dates.");
        }
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setUser(user);
        leaveRequest.setStartDate(dto.getStartDate());
        leaveRequest.setEndDate(dto.getEndDate());
        leaveRequest.setRole("ROLE_" + user.getRole().name());
        leaveRequest.setReason(dto.getReason());
        leaveRequest.setStatus(LeaveStatus.PENDING);
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        logger.info("Leave request {} created for user {}", savedRequest.getId(), user.getId());
        return toDto(savedRequest);
    }

    public List<LeaveRequestDTO> getAllLeaveRequests() {
        return toDtos(leaveRequestRepository.findAll());
    }

    public LeaveRequestDTO getLeaveRequestById(Long id) {
        LeaveRequest leave = findLeave(id);
        currentUserService.requireSelfOrRole(leave.getUser().getId(), Role.ADMIN);
        return toDto(leave);
    }

    public List<LeaveRequestDTO> getLeavesByUser(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        return toDtos(leaveRequestRepository.findByUserId(userId));
    }

    public List<LeaveRequestDTO> getLeavesByUserAndStatus(Long userId, LeaveStatus status) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        return toDtos(leaveRequestRepository.findByUserIdAndStatus(userId, status));
    }

    public List<LeaveRequestDTO> getAllLeavesByStatus(LeaveStatus status) {
        return toDtos(this.leaveRequestRepository.findAllByStatus(status));
    }

    /** The owner can change dates/reason while the request is still pending. */
    public LeaveRequestDTO updateLeaveRequest(Long id, LeaveRequestDTO dto) {
        LeaveRequest existing = findLeave(id);
        requireOwner(existing);
        if (existing.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only pending leave requests can be changed");
        }
        validateDates(dto.getStartDate(), dto.getEndDate());
        if (leaveRequestRepository.existsOverlapping(existing.getUser().getId(), dto.getStartDate(), dto.getEndDate(), id)) {
            throw new ConflictException("Leave Request already exists for these dates! Choose different dates.");
        }
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setReason(dto.getReason());
        return toDto(leaveRequestRepository.save(existing));
    }

    /**
     * Admin decision on a pending leave. Approving a doctor's leave publishes
     * {@link DoctorLeaveApprovedEvent}: in the same transaction the scheduling module blocks the
     * slots and the appointments module cancels the bookings and notifies the patients.
     */
    @Transactional
    public LeaveRequestDTO updateLeaveStatus(Long id, LeaveStatus status) {
        LeaveRequest leave = findLeave(id);
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("This leave request has already been " + leave.getStatus().name().toLowerCase());
        }
        if (status == LeaveStatus.PENDING) {
            throw new BadRequestException("Status must be APPROVED or REJECTED");
        }
        leave.setStatus(status);
        leaveRequestRepository.save(leave);
        if (status == LeaveStatus.APPROVED && leave.getUser().getRole() == Role.DOCTOR) {
            doctorRepository.findByUserId(leave.getUser().getId())
                    .ifPresent(doctor -> eventPublisher.publishEvent(
                            new DoctorLeaveApprovedEvent(doctor.getId(), leave.getStartDate(), leave.getEndDate())));
        }
        logger.info("Leave request {} {}", id, status);
        return toDto(leave);
    }

    /** The owner can withdraw a pending request; an admin can delete any request. */
    public boolean deleteLeaveRequest(Long id) {
        LeaveRequest leaveRequest = findLeave(id);
        if (!currentUserService.hasRole(Role.ADMIN)) {
            requireOwner(leaveRequest);
            if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
                throw new BadRequestException("Only pending leave requests can be withdrawn");
            }
        }
        leaveRequestRepository.delete(leaveRequest);
        return true;
    }

    private static void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BadRequestException("Start and end date are required");
        }
        if (end.isBefore(start)) {
            throw new BadRequestException("End date cannot be before start date");
        }
        if (start.isBefore(LocalDate.now())) {
            throw new BadRequestException("Leave cannot start in the past");
        }
    }

    private void requireOwner(LeaveRequest leave) {
        if (!Objects.equals(leave.getUser().getId(), currentUserService.getCurrentUserId())) {
            throw new ForbiddenException("You can only change your own leave requests");
        }
    }

    private LeaveRequest findLeave(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
    }

    private static List<LeaveRequestDTO> toDtos(List<LeaveRequest> leaves) {
        return leaves.stream().map(LeaveRequestService::toDto).toList();
    }

    static LeaveRequestDTO toDto(LeaveRequest leave) {
        User user = leave.getUser();
        String role = user != null ? "ROLE_" + user.getRole().name() : leave.getRole();
        return new LeaveRequestDTO(leave.getId(), user != null ? user.getId() : null, role,
                leave.getStartDate(), leave.getEndDate(), leave.getReason(), leave.getStatus());
    }

    // Module API (account deletion): removes all leave requests of a user
    @Transactional
    public void deleteAllForUser(Long userId) {
        leaveRequestRepository.deleteByUserId(userId);
    }
}
