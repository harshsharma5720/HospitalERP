package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.LeaveRequestDTO;
import ITmonteur.example.hospitalERP.entities.LeaveStatus;
import ITmonteur.example.hospitalERP.services.LeaveRequestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Staff (doctor / receptionist / admin) leave requests. Ownership is checked in LeaveRequestService.
@RestController
@RequestMapping("/api/leaves")
public class LeaveRequestController {

    private static final Logger logger = LoggerFactory.getLogger(LeaveRequestController.class);

    @Autowired
    private LeaveRequestService leaveRequestService;

    // APPLY FOR LEAVE (always for the logged-in user)
    @PostMapping("/apply")
    public ResponseEntity<LeaveRequestDTO> applyLeave(@Valid @RequestBody LeaveRequestDTO leaveRequestDTO) {
        return ResponseEntity.ok(leaveRequestService.createLeaveRequest(leaveRequestDTO));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaveRequestDTO>> getAllLeaves() {
        return ResponseEntity.ok(leaveRequestService.getAllLeaveRequests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestDTO> getLeaveById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LeaveRequestDTO>> getLeavesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(leaveRequestService.getLeavesByUser(userId));
    }

    @GetMapping("/pending/{userId}")
    public ResponseEntity<List<LeaveRequestDTO>> getPendingLeaves(@PathVariable Long userId) {
        return ResponseEntity.ok(this.leaveRequestService.getLeavesByUserAndStatus(userId, LeaveStatus.PENDING));
    }

    @GetMapping("/approved/{userId}")
    public ResponseEntity<List<LeaveRequestDTO>> getApprovedLeaves(@PathVariable Long userId) {
        return ResponseEntity.ok(this.leaveRequestService.getLeavesByUserAndStatus(userId, LeaveStatus.APPROVED));
    }

    @GetMapping("/rejected/{userId}")
    public ResponseEntity<List<LeaveRequestDTO>> getRejectedLeaves(@PathVariable Long userId) {
        return ResponseEntity.ok(this.leaveRequestService.getLeavesByUserAndStatus(userId, LeaveStatus.REJECTED));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveRequestDTO> updateLeave(@PathVariable Long id,
                                                       @Valid @RequestBody LeaveRequestDTO dto) {
        return ResponseEntity.ok(leaveRequestService.updateLeaveRequest(id, dto));
    }

    // Approve / reject — admin only, so staff cannot approve their own leave
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveRequestDTO> updateLeaveStatus(@PathVariable Long id,
                                                             @RequestParam LeaveStatus status) {
        logger.info("Updating status of leave request {} to {}", id, status);
        return ResponseEntity.ok(leaveRequestService.updateLeaveStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteLeave(@PathVariable Long id) {
        leaveRequestService.deleteLeaveRequest(id);
        return ResponseEntity.ok("Leave Request deleted successfully");
    }
}
