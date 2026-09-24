package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.*;
import ITmonteur.example.hospitalERP.entities.LeaveStatus;
import ITmonteur.example.hospitalERP.services.AdminService;
import ITmonteur.example.hospitalERP.services.DoctorService;
import ITmonteur.example.hospitalERP.services.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

// Everything under /api/admin requires ROLE_ADMIN (see SecurityConfig)
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private LeaveRequestService leaveRequestService;
    @Autowired
    private DoctorService doctorService;

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    // -------------------- Users of any role --------------------
    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        logger.info("Admin creating {} account: {}", registerRequestDTO.getRole(), registerRequestDTO.getUsername());
        return ResponseEntity.ok(this.adminService.createUser(registerRequestDTO));
    }

    // -------------------- Patient --------------------
    @PostMapping("/patient")
    public ResponseEntity<PtInfoDTO> createPatient(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        return ResponseEntity.ok(this.adminService.createPatient(registerRequestDTO));
    }

    // -------------------- Doctor --------------------
    @PostMapping("/doctor")
    public ResponseEntity<DoctorDTO> createDoctor(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        return ResponseEntity.ok(this.adminService.createDoctor(registerRequestDTO));
    }

    // -------------------- Receptionist --------------------
    @PostMapping("/receptionist")
    public ResponseEntity<ReceptionistDTO> createReceptionist(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        return ResponseEntity.ok(this.adminService.createReceptionist(registerRequestDTO));
    }

    // -------------------- Leaves --------------------
    @PutMapping("/approve/{leaveId}")
    public ResponseEntity<LeaveRequestDTO> approveLeave(@PathVariable Long leaveId) {
        logger.info("Approving leave {}", leaveId);
        return ResponseEntity.ok(this.adminService.approveLeave(leaveId));
    }

    @PutMapping("/reject/{leaveId}")
    public ResponseEntity<LeaveRequestDTO> rejectLeave(@PathVariable Long leaveId) {
        logger.info("Rejecting leave {}", leaveId);
        return ResponseEntity.ok(this.adminService.rejectLeave(leaveId));
    }

    @GetMapping("/allApproved")
    public ResponseEntity<List<LeaveRequestDTO>> getAllApprovedLeaves() {
        return ResponseEntity.ok(this.leaveRequestService.getAllLeavesByStatus(LeaveStatus.APPROVED));
    }

    @GetMapping("/allPending")
    public ResponseEntity<List<LeaveRequestDTO>> getAllPendingLeaves() {
        return ResponseEntity.ok(this.leaveRequestService.getAllLeavesByStatus(LeaveStatus.PENDING));
    }

    @GetMapping("/allRejected")
    public ResponseEntity<List<LeaveRequestDTO>> getAllRejectedLeaves() {
        return ResponseEntity.ok(this.leaveRequestService.getAllLeavesByStatus(LeaveStatus.REJECTED));
    }

    // -------------------- Doctor appointments --------------------
    @GetMapping("/doctorPendingAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getPendingAppointmentsForDoctor(@PathVariable Long userId) {
        return ResponseEntity.ok(this.doctorService.getAllPendingAppointmentsByDoctorId(userId));
    }

    @GetMapping("/doctorCompletedAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getCompletedAppointmentsForDoctor(@PathVariable Long userId) {
        return ResponseEntity.ok(this.doctorService.getAllCompletedAppointmentsByDoctorId(userId));
    }

    // Note: takes the doctor's own id (doctor.id), not the user id
    @GetMapping("/doctorAppointmentCount/{doctorId}")
    public ResponseEntity<Map<String, Long>> getAppointmentCount(@PathVariable Long doctorId) {
        return ResponseEntity.ok(Map.of(
                "pending", this.doctorService.getPendingCount(doctorId),
                "completed", this.doctorService.getCompletedCount(doctorId)));
    }

    // -------------------- Users --------------------
    @GetMapping("/allUsers")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(this.adminService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(this.adminService.getUserById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteUserById(@PathVariable Long id) {
        logger.info("Admin deleting user {}", id);
        return ResponseEntity.ok(this.adminService.deleteUserById(id));
    }
}
