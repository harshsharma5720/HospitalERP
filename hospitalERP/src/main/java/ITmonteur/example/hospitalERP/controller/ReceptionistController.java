package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.dto.ReceptionistDTO;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.services.ReceptionistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/receptionist")
public class ReceptionistController {

    private static final Logger logger = LoggerFactory.getLogger(ReceptionistController.class);

    @Autowired
    private ReceptionistService receptionistService;

    // Get all receptionists
    @GetMapping("/getAll")
    public ResponseEntity<List<ReceptionistDTO>> getAllReceptionist() {
        logger.info("GET request received to fetch all receptionists");
        List<ReceptionistDTO> receptionistDTOList = receptionistService.getAllReceptionist();
        logger.info("Total receptionists retrieved: {}", receptionistDTOList.size());
        return ResponseEntity.ok(receptionistDTOList);
    }

    // Get receptionist by ID
    @GetMapping("/getReceptionist/{receptionistId}")
    public ResponseEntity<ReceptionistDTO> getReceptionistByID(@PathVariable long receptionistId) {
        logger.info("GET request received for receptionist ID: {}", receptionistId);
        try {
            ReceptionistDTO receptionistDTO = receptionistService.getReceptionistByID(receptionistId);
            logger.info("Receptionist retrieved: {}", receptionistDTO.getName());
            return ResponseEntity.ok(receptionistDTO);
        } catch (ResourceNotFoundException e) {
            logger.warn("Receptionist not found with ID: {}", receptionistId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            logger.error("Error fetching receptionist ID {}: {}", receptionistId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Get all appointments
    @GetMapping("/getAppointments")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        logger.info("GET request received to fetch all appointments");
        List<AppointmentDTO> appointmentDTOList = receptionistService.getAllAppointments();
        logger.info("Total appointments retrieved: {}", appointmentDTOList.size());
        return ResponseEntity.ok(appointmentDTOList);
    }

    // Get appointments by doctor
    @GetMapping("/getAppointmentByDoctor/{doctorName}")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointmentsOfDoctor(@PathVariable String doctorName) {
        logger.info("GET request received for appointments of doctor: {}", doctorName);
        try {
            List<AppointmentDTO> appointmentDTOList = receptionistService.getAllAppointmentsByDoctorName(doctorName);
            logger.info("Appointments retrieved for doctor {}: {}", doctorName, appointmentDTOList.size());
            return ResponseEntity.ok(appointmentDTOList);
        } catch (Exception e) {
            logger.error("Error fetching appointments for doctor {}: {}", doctorName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Create new appointment
    @PostMapping("/NewAppointment")
    public ResponseEntity<String> createNewAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        logger.info("POST request received to create appointment for patient: {}", appointmentDTO.getPatientName());
        boolean success = receptionistService.createAppointment(appointmentDTO);
        if (success) {
            logger.info("Appointment created successfully for patient: {}", appointmentDTO.getPatientName());
            return ResponseEntity.ok("Your slot has been booked");
        } else {
            logger.error("Failed to create appointment for patient: {}", appointmentDTO.getPatientName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while booking slot");
        }
    }

    // Delete receptionist
    @DeleteMapping("/delete/{receptionistId}")
    public ResponseEntity<String> deleteReceptionist(@PathVariable Long receptionistId) {
        logger.info("DELETE request received for receptionist ID: {}", receptionistId);
        boolean deleted = receptionistService.deleteReceptionist(receptionistId);
        if (deleted) {
            logger.info("Receptionist deleted successfully with ID: {}", receptionistId);
            return ResponseEntity.ok("Receptionist deleted successfully!");
        } else {
            logger.warn("Receptionist not found or deletion failed for ID: {}", receptionistId);
            return ResponseEntity.status(404).body("Receptionist not found or could not be deleted.");
        }
    }

    // Delete appointment
    @DeleteMapping("/deleteAppointment/{appointmentId}")
    public ResponseEntity<String> deleteAppointment(@PathVariable Long appointmentId) {
        logger.info("DELETE request received for appointment ID: {}", appointmentId);
        boolean deleted = receptionistService.deleteAppointment(appointmentId);
        if (deleted) {
            logger.info("Appointment deleted successfully with ID: {}", appointmentId);
            return ResponseEntity.ok("Appointment deleted successfully!");
        } else {
            logger.warn("Appointment not found or deletion failed for ID: {}", appointmentId);
            return ResponseEntity.status(404).body("Appointment not found or could not be deleted.");
        }
    }

    // Delete all appointments
    @DeleteMapping("/deleteAllAppointments")
    public ResponseEntity<String> deleteAllAppointments() {
        logger.info("DELETE request received to delete all appointments");
        boolean deleted = receptionistService.deleteAllAppointments();
        if (deleted) {
            logger.info("All appointments deleted successfully");
            return ResponseEntity.ok("Appointments deleted successfully!");
        } else {
            logger.warn("No appointments found to delete");
            return ResponseEntity.status(404).body("Appointments not found or could not be deleted.");
        }
    }

    // Delete all receptionists
    @DeleteMapping("/receptionists")
    public ResponseEntity<String> deleteAllReceptionists() {
        logger.info("DELETE request received to delete all receptionists");
        boolean isDeleted = receptionistService.deleteAllReceptionist();
        if (isDeleted) {
            logger.info("All receptionists deleted successfully");
            return ResponseEntity.ok("All receptionists deleted successfully.");
        } else {
            logger.warn("No receptionists found to delete");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to delete receptionists or no receptionists found.");
        }
    }

    // Update receptionist
    @PutMapping("/{id}")
    public ResponseEntity<ReceptionistDTO> updateReceptionist(
            @PathVariable("id") Long id,
            @RequestBody ReceptionistDTO receptionistDTO) {

        logger.info("PUT request received to update receptionist with ID: {}", id);

        try {
            ReceptionistDTO updatedReceptionist = receptionistService.updateReceptionist(id, receptionistDTO);
            logger.info("Receptionist updated successfully: {}", updatedReceptionist.getName());
            return ResponseEntity.ok(updatedReceptionist);
        } catch (ResourceNotFoundException e) {
            logger.warn("Receptionist not found with ID: {}", id);
            return ResponseEntity.status(404).body(null);
        } catch (Exception e) {
            logger.error("Error updating receptionist with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
