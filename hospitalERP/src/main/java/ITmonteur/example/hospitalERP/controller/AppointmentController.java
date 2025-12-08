package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.services.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/appointment")
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    @Autowired
    private AppointmentService appointmentService;

    // Get all appointments
    @GetMapping("/getAll")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        logger.info("Fetching all appointments");
        List<AppointmentDTO> appointmentDTOList = this.appointmentService.getAllAppointments();
        logger.info("Total appointments fetched: {}", appointmentDTOList.size());
        return ResponseEntity.ok(appointmentDTOList);
    }

    @GetMapping("/getPatientAppointments")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentsForLoggedInPatient(
            @RequestHeader("Authorization") String token) {
        logger.info("Fetching appointments for logged-in patient using token");
        List<AppointmentDTO> appointments = appointmentService.getAppointmentsByToken(token);
        logger.info("Fetching All Appointments with token :{}",token);
        return ResponseEntity.ok(appointments);
    }


//    // Get all appointments by Patient ID
//    @GetMapping("/appointmentsByPatient/{patientId}")
//    public ResponseEntity<List<AppointmentDTO>> getAppointmentsByPatientId(@PathVariable Long patientId) {
//        logger.info("Fetching all appointments for patient ID: {}", patientId);
//        List<AppointmentDTO> appointmentDTOList = this.appointmentService.getAppointmentsByPatientId(patientId);
//        logger.info("Total appointments found for patient ID {}: {}", patientId, appointmentDTOList.size());
//        return ResponseEntity.ok(appointmentDTOList);
//    }


    // Get appointment by ID
    @GetMapping("appointmentId/{myId}")
    public ResponseEntity<AppointmentDTO> getAppointmentByAppointmentID(@PathVariable long myId) {
        logger.info("Fetching appointment with ID: {}", myId);
        AppointmentDTO appointmentDTO = this.appointmentService.getAppointmentByID(myId);
        return ResponseEntity.ok(appointmentDTO);
    }

    @GetMapping("/patientCompletedAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getAllPatientCompletedAppointments(@PathVariable Long userId) {
        logger.info("Fetching completed appointments for patient ID: {}", userId);
        List<AppointmentDTO> appointmentDTOList = this.appointmentService.getAllPatientCompletedAppointments(userId);
        logger.info("Total completed appointments found for patient ID {}: {}", userId, appointmentDTOList.size());
        return ResponseEntity.ok(appointmentDTOList);
    }
    @GetMapping("/patientPendingAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getAllPatientPendingAppointments(@PathVariable Long userId) {
        logger.info("Fetching pending appointments for patient ID: {}", userId);
        List<AppointmentDTO> appointmentDTOList = this.appointmentService.getAllPatientPendingAppointments(userId);
        logger.info("Total pending appointments found for patient ID {}: {}", userId, appointmentDTOList.size());
        return ResponseEntity.ok(appointmentDTOList);
    }

    @GetMapping("/getDoctorAppointments")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentsForLoggedInDoctor(
            @RequestHeader("Authorization") String token) {
        logger.info("Fetching appointments for logged-in doctor using token");
        List<AppointmentDTO> appointments = appointmentService.getAppointmentsForDoctor(token);
        logger.info("Appointments fetched for doctor using token");
        return ResponseEntity.ok(appointments);
    }

    // Get all appointments by doctor name
    @GetMapping("appointmentsByDoctor/{doctorName}")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointmentsByDoctor(@PathVariable String doctorName) {
        logger.info("Fetching appointments for doctor: {}", doctorName);
        List<AppointmentDTO> appointmentDTOList = this.appointmentService.getAppointmentsByDrName(doctorName);
        logger.info("Total appointments found for doctor {}: {}", doctorName, appointmentDTOList.size());
        return ResponseEntity.ok(appointmentDTOList);
    }

    @GetMapping("/allPendingAppointments")
    public ResponseEntity<List<AppointmentDTO>> getAllPendingAppointments() {
        logger.info("Fetching all pending appointments");
        List<AppointmentDTO> appointmentDTOList = this.appointmentService.getAllPendingAppointments();
        logger.info("Total pending appointments fetched: {}", appointmentDTOList.size());
        return ResponseEntity.ok(appointmentDTOList);
    }

    @GetMapping("/allCompletedAppointments")
    public ResponseEntity<List<AppointmentDTO>> getAllCompletedAppointments() {
        logger.info("Fetching all completed appointments");
        List<AppointmentDTO> appointmentDTOList = this.appointmentService.getAllCompletedAppointments();
        logger.info("Total completed appointments fetched: {}", appointmentDTOList.size());
        return ResponseEntity.ok(appointmentDTOList);
    }

    // Create a new appointment
    @PostMapping("/NewAppointment")
    public ResponseEntity<String> createNewAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        logger.info("Creating new appointment for patient: {}", appointmentDTO.getPatientName());
        if (this.appointmentService.createAppointment(appointmentDTO)) {
            logger.info("Appointment booked successfully for patient: {}", appointmentDTO.getPatientName());
            return ResponseEntity.ok("Your slot has been booked");
        } else {
            logger.error("Error occurred while booking appointment for patient: {}", appointmentDTO.getPatientName());
            return ResponseEntity.ok("Error occured");
        }
    }

    // Delete an appointment by ID
    @DeleteMapping("/CancelAppointment/{appointmentId}")
    public ResponseEntity<String> deleteAppointmentByAppointmentId(@PathVariable long appointmentId) {
        logger.info("Deleting appointment with ID: {}", appointmentId);
        if (this.appointmentService.deleteAppointmentByID(appointmentId)) {
            logger.info("Appointment with ID {} deleted successfully", appointmentId);
            return ResponseEntity.ok("Your appointment slot has been deleted successfully!!");
        } else {
            logger.warn("Failed to delete appointment with ID: {}", appointmentId);
            return ResponseEntity.ok("you entered the wrong credentials..");
        }
    }

    // Delete all appointments
    @DeleteMapping("/cancelALlAppointments")
    public ResponseEntity<String> deleteAllAppointment() {
        logger.info("Deleting all appointments");
        if (this.appointmentService.deleteAllAppointments()) {
            logger.info("All appointments deleted successfully");
            return ResponseEntity.ok("Deleted all appointments successfully!!");
        } else {
            logger.warn("Failed to delete all appointments");
            return ResponseEntity.ok("Error occured..");
        }
    }

    // Update an appointment
    @PutMapping("/update/{appointmentId}")
    public ResponseEntity<AppointmentDTO> updateAppointment(@PathVariable long appointmentId,
                                                            @RequestBody AppointmentDTO appointmentDTO) {
        logger.info("Updating appointment with ID: {}", appointmentId);
        AppointmentDTO appointmentDTO1 = this.appointmentService.updateAppointmentById(appointmentId, appointmentDTO);
        logger.info("Appointment updated successfully for ID: {}", appointmentId);
        return ResponseEntity.ok(appointmentDTO1);
    }
}
