package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.*;
import ITmonteur.example.hospitalERP.entities.LeaveRequest;
import ITmonteur.example.hospitalERP.entities.LeaveStatus;
import ITmonteur.example.hospitalERP.services.AdminService;
import ITmonteur.example.hospitalERP.services.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private LeaveRequestService leaveRequestService;

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    // -------------------- Patient --------------------
    @PostMapping("/patient")
    public ResponseEntity<PtInfoDTO> createPatient(@RequestBody RegisterRequestDTO registerRequestDTO) {
        logger.info("Request received to create patient: {}", registerRequestDTO.getUsername());
        PtInfoDTO createdPatient = this.adminService.createPatient(registerRequestDTO);
        logger.info("Patient created successfully with username: {}", createdPatient.getUserName());
        return ResponseEntity.ok(createdPatient);
    }

    // -------------------- Doctor --------------------
    @PostMapping("/doctor")
    public ResponseEntity<DoctorDTO> createDoctor(@RequestBody RegisterRequestDTO registerRequestDTO) {
        logger.info("Request received to create doctor: {}", registerRequestDTO.getUsername());
        DoctorDTO createdDoctor = this.adminService.createDoctor(registerRequestDTO);
        logger.info("Doctor created successfully with username: {}", createdDoctor.getUserName());
        return ResponseEntity.ok(createdDoctor);
    }

    // -------------------- Receptionist --------------------
    @PostMapping("/receptionist")
    public ResponseEntity<ReceptionistDTO> createReceptionist(@RequestBody RegisterRequestDTO registerRequestDTO) {
        logger.info("Request received to create receptionist: {}", registerRequestDTO.getUsername());
        ReceptionistDTO createdReceptionist = this.adminService.createReceptionist(registerRequestDTO);
        logger.info("Receptionist created successfully with username: {}", createdReceptionist.getUserName());
        return ResponseEntity.ok(createdReceptionist);
    }

    @PutMapping("/approve/{leaveId}")
    public ResponseEntity<?> approveLeave(@PathVariable Long leaveId) {
        LeaveRequest updatedLeave = this.adminService.approveLeave(leaveId);
        logger.info("Approving leave for leaveId: {}", leaveId);
        return ResponseEntity.ok(updatedLeave);
    }

    @GetMapping("/allApproved")
    public ResponseEntity<List<LeaveRequestDTO>> getAllApprovedLeaves() {
        logger.info("Fetching all approved leaves");
        List<LeaveRequestDTO> leaves = this.leaveRequestService.getAllLeavesByStatus(LeaveStatus.APPROVED);
        return ResponseEntity.ok(leaves);
    }

    @GetMapping("/allPending")
    public ResponseEntity<List<LeaveRequestDTO>> getAllPendingLeaves() {
        logger.info("Fetching all pending leaves");
        List<LeaveRequestDTO> leaves = this.leaveRequestService.getAllLeavesByStatus(LeaveStatus.PENDING);
        return ResponseEntity.ok(leaves);
    }

    // -------------------- Appointment --------------------
//    @PostMapping("/appointment")
//    public ResponseEntity<AppointmentDTO> createAppointment(@RequestBody AppointmentDTO appointmentDTO) {
//        logger.info("Request received to create appointment for patient ID: {}", appointmentDTO.getPtInfoId());
//        AppointmentDTO createdAppointment = adminService.createAppointment(appointmentDTO);
//        logger.info("Appointment created successfully with ID: {}", createdAppointment.getAppointmentID());
//        return ResponseEntity.ok(createdAppointment);
//    }
}
