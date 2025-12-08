package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.LeaveRequestDTO;
import ITmonteur.example.hospitalERP.entities.LeaveStatus;
import ITmonteur.example.hospitalERP.services.LeaveRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/leaves")
public class LeaveRequestController {

    private static final Logger logger = LoggerFactory.getLogger(LeaveRequestController.class);

    @Autowired
    private LeaveRequestService leaveRequestService;

    // APPLY FOR LEAVE
    @PostMapping("/apply")
    public ResponseEntity<LeaveRequestDTO> applyLeave(@RequestBody LeaveRequestDTO leaveRequestDTO) {
        logger.info("API Call: Applying Leave for User {}", leaveRequestDTO.getUserId());
        return ResponseEntity.ok(leaveRequestService.createLeaveRequest(leaveRequestDTO));
    }

    @GetMapping("/all")
    public ResponseEntity<List<LeaveRequestDTO>> getAllLeaves() {
        logger.info("API Call: Fetching All Leave Requests");
        return ResponseEntity.ok(leaveRequestService.getAllLeaveRequests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestDTO> getLeaveById(@PathVariable Long id) {
        logger.info("API Call: Fetching Leave Request By ID {}", id);
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LeaveRequestDTO>> getLeavesByUser(@PathVariable Long userId) {
        logger.info("API Call: Fetching Leave Requests for User {}", userId);
        return ResponseEntity.ok(leaveRequestService.getLeavesByUser(userId));
    }
    @GetMapping("/pending/{userId}")
    public ResponseEntity<List<LeaveRequestDTO>> getPendingLeaves(@PathVariable Long userId) {
        logger.info("API Call: Fetching pending leaves for user {}", userId);
        List<LeaveRequestDTO> leaves = this.leaveRequestService.getLeavesByUserAndStatus(userId, LeaveStatus.PENDING);
        return ResponseEntity.ok(leaves);
    }

    @GetMapping("/approved/{userId}")
    public ResponseEntity<List<LeaveRequestDTO>> getApprovedLeaves(@PathVariable Long userId) {
        logger.info("API Call: Fetching approved leaves for user {}", userId);
        List<LeaveRequestDTO> leaves = this.leaveRequestService.getLeavesByUserAndStatus(userId, LeaveStatus.APPROVED);
        return ResponseEntity.ok(leaves);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveRequestDTO> updateLeave(@PathVariable Long id,
                                                       @RequestBody LeaveRequestDTO dto) {
        logger.info("API Call: Updating Leave Request ID {}", id);
        return ResponseEntity.ok(leaveRequestService.updateLeaveRequest(id, dto));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<LeaveRequestDTO> updateLeaveStatus(@PathVariable Long id,
                                                             @RequestParam LeaveStatus status) {
        logger.info("API Call: Updating Leave Status ID {}", id);
        return ResponseEntity.ok(leaveRequestService.updateLeaveStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteLeave(@PathVariable Long id) {
        logger.info("API Call: Deleting Leave Request ID {}", id);
        leaveRequestService.deleteLeaveRequest(id);
        return ResponseEntity.ok("Leave Request deleted successfully");
    }
}
