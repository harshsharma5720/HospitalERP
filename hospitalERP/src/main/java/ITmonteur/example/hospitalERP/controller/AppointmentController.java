package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.services.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Access rules: see SecurityConfig ("/appointment/**") and the ownership checks in AppointmentService
@RestController
@RequestMapping("/appointment")
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    @Autowired
    private AppointmentService appointmentService;

    // Get all appointments (admin / receptionist)
    @GetMapping("/getAll")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        return ResponseEntity.ok(this.appointmentService.getAllAppointments());
    }

    // Appointments of the logged-in patient
    @GetMapping("/getPatientAppointments")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentsForLoggedInPatient() {
        return ResponseEntity.ok(this.appointmentService.getMyAppointments());
    }

    // Get appointment by ID (owner patient, its doctor, or staff)
    @GetMapping("/appointmentId/{myId}")
    public ResponseEntity<AppointmentDTO> getAppointmentByAppointmentID(@PathVariable long myId) {
        return ResponseEntity.ok(this.appointmentService.getAppointmentByID(myId));
    }

    @GetMapping("/patientCompletedAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getAllPatientCompletedAppointments(@PathVariable Long userId) {
        return ResponseEntity.ok(this.appointmentService.getAllPatientCompletedAppointments(userId));
    }

    @GetMapping("/patientPendingAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getAllPatientPendingAppointments(@PathVariable Long userId) {
        return ResponseEntity.ok(this.appointmentService.getAllPatientPendingAppointments(userId));
    }

    // Appointments of the logged-in doctor
    @GetMapping("/getDoctorAppointments")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentsForLoggedInDoctor() {
        return ResponseEntity.ok(this.appointmentService.getAppointmentsForDoctor());
    }

    // Get all appointments by doctor name (admin / receptionist)
    @GetMapping("/appointmentsByDoctor/{doctorName}")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointmentsByDoctor(@PathVariable String doctorName) {
        return ResponseEntity.ok(this.appointmentService.getAppointmentsByDrName(doctorName));
    }

    @GetMapping("/allPendingAppointments")
    public ResponseEntity<List<AppointmentDTO>> getAllPendingAppointments() {
        return ResponseEntity.ok(this.appointmentService.getAllPendingAppointments());
    }

    @GetMapping("/allCompletedAppointments")
    public ResponseEntity<List<AppointmentDTO>> getAllCompletedAppointments() {
        return ResponseEntity.ok(this.appointmentService.getAllCompletedAppointments());
    }

    // Book a new appointment
    @PostMapping("/NewAppointment")
    public ResponseEntity<AppointmentDTO> createNewAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        AppointmentDTO created = this.appointmentService.createAppointment(appointmentDTO);
        logger.info("Appointment {} booked", created.getAppointmentID());
        return ResponseEntity.ok(created);
    }

    // Cancel an appointment (kept in history with a CANCELLED status)
    @DeleteMapping("/CancelAppointment/{appointmentId}")
    public ResponseEntity<String> cancelAppointment(@PathVariable long appointmentId) {
        this.appointmentService.cancelAppointment(appointmentId);
        return ResponseEntity.ok("Your appointment has been cancelled successfully!");
    }

    // Update / reschedule an appointment
    @PutMapping("/update/{appointmentId}")
    public ResponseEntity<AppointmentDTO> updateAppointment(@PathVariable long appointmentId,
                                                            @RequestBody AppointmentDTO appointmentDTO) {
        return ResponseEntity.ok(this.appointmentService.updateAppointmentById(appointmentId, appointmentDTO));
    }
}
