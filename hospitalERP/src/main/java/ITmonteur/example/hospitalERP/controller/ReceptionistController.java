package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.dto.ReceptionistDTO;
import ITmonteur.example.hospitalERP.services.AppointmentService;
import ITmonteur.example.hospitalERP.services.DoctorService;
import ITmonteur.example.hospitalERP.services.ReceptionistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Everything under /api/receptionist requires RECEPTIONIST or ADMIN (see SecurityConfig)
@RestController
@RequestMapping("/api/receptionist")
public class ReceptionistController {

    @Autowired
    private ReceptionistService receptionistService;
    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private DoctorService doctorService;

    // Get all receptionists
    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReceptionistDTO>> getAllReceptionist() {
        return ResponseEntity.ok(this.receptionistService.getAllReceptionist());
    }

    // Get receptionist by user ID (self or admin)
    @GetMapping("/getReceptionist/{receptionistId}")
    public ResponseEntity<ReceptionistDTO> getReceptionistByID(@PathVariable long receptionistId) {
        return ResponseEntity.ok(this.receptionistService.getReceptionistByID(receptionistId));
    }

    // Get all appointments
    @GetMapping("/getAppointments")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        return ResponseEntity.ok(this.appointmentService.getAllAppointments());
    }

    // Get appointments by doctor name
    @GetMapping("/getAppointmentByDoctor/{doctorName}")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointmentsOfDoctor(@PathVariable String doctorName) {
        return ResponseEntity.ok(this.appointmentService.getAppointmentsByDrName(doctorName));
    }

    // Book on behalf of a patient (ptInfoId and slotId are required)
    @PostMapping("/NewAppointment")
    public ResponseEntity<AppointmentDTO> createNewAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        return ResponseEntity.ok(this.appointmentService.createAppointment(appointmentDTO));
    }

    @GetMapping("/doctorPendingAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getPendingAppointmentsForDoctor(@PathVariable Long userId) {
        return ResponseEntity.ok(this.doctorService.getAllPendingAppointmentsByDoctorId(userId));
    }

    @GetMapping("/doctorCompletedAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getCompletedAppointmentsForDoctor(@PathVariable Long userId) {
        return ResponseEntity.ok(this.doctorService.getAllCompletedAppointmentsByDoctorId(userId));
    }

    // Delete receptionist by receptionist ID
    @DeleteMapping("/delete/{receptionistId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteReceptionist(@PathVariable Long receptionistId) {
        this.receptionistService.deleteReceptionist(receptionistId);
        return ResponseEntity.ok("Receptionist deleted successfully!");
    }

    // Cancel an appointment (kept in history with a CANCELLED status)
    @DeleteMapping("/deleteAppointment/{appointmentId}")
    public ResponseEntity<String> deleteAppointment(@PathVariable Long appointmentId) {
        this.appointmentService.cancelAppointment(appointmentId);
        return ResponseEntity.ok("Appointment cancelled successfully!");
    }

    // Update receptionist by user ID (self or admin)
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ReceptionistDTO> updateReceptionist(
            @PathVariable("id") Long id,
            @RequestPart("receptionistDTO") ReceptionistDTO receptionistDTO,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        return ResponseEntity.ok(this.receptionistService.updateReceptionist(id, receptionistDTO, profileImage));
    }
}
