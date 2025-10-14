package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.*;
import ITmonteur.example.hospitalERP.services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    // -------------------- Patient --------------------
    @PostMapping("/patient")
    public ResponseEntity<PtInfoDTO> createPatient(@RequestBody PtInfoDTO ptInfoDTO) {
        logger.info("Request received to create patient: {}", ptInfoDTO.getPatientName());
        PtInfoDTO createdPatient = adminService.createPtInfo(ptInfoDTO);
        logger.info("Patient created successfully with username: {}", createdPatient.getUserName());
        return ResponseEntity.ok(createdPatient);
    }

    // -------------------- Doctor --------------------
    @PostMapping("/doctor")
    public ResponseEntity<DoctorDTO> createDoctor(@RequestBody DoctorDTO doctorDTO) {
        logger.info("Request received to create doctor: {}", doctorDTO.getName());
        DoctorDTO createdDoctor = adminService.createDoctor(doctorDTO);
        logger.info("Doctor created successfully with username: {}", createdDoctor.getUserName());
        return ResponseEntity.ok(createdDoctor);
    }

    // -------------------- Receptionist --------------------
    @PostMapping("/receptionist")
    public ResponseEntity<ReceptionistDTO> createReceptionist(@RequestBody ReceptionistDTO receptionistDTO) {
        logger.info("Request received to create receptionist: {}", receptionistDTO.getName());
        ReceptionistDTO createdReceptionist = adminService.createReceptionist(receptionistDTO);
        logger.info("Receptionist created successfully with username: {}", createdReceptionist.getUserName());
        return ResponseEntity.ok(createdReceptionist);
    }

    // -------------------- Appointment --------------------
    @PostMapping("/appointment")
    public ResponseEntity<AppointmentDTO> createAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        logger.info("Request received to create appointment for patient ID: {}", appointmentDTO.getPtInfoId());
        AppointmentDTO createdAppointment = adminService.createAppointment(appointmentDTO);
        logger.info("Appointment created successfully with ID: {}", createdAppointment.getAppointmentID());
        return ResponseEntity.ok(createdAppointment);
    }
}
