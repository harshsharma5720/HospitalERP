package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.services.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * A doctor's appointment endpoints. The URLs live under /api/doctor for the frontend,
 * but the logic belongs to the appointments module. Access: DOCTOR or ADMIN (SecurityConfig),
 * ownership checked in AppointmentService.
 */
@RestController
@RequestMapping("/api/doctor")
public class DoctorAppointmentController {

    private final AppointmentService appointmentService;

    public DoctorAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PutMapping("/complete/{appointmentId}")
    public ResponseEntity<String> markAppointmentCompleted(@PathVariable long appointmentId) {
        return ResponseEntity.ok(appointmentService.markAsCompleted(appointmentId));
    }

    @GetMapping("/doctorPendingAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getPendingAppointmentsForDoctor(@PathVariable Long userId) {
        return ResponseEntity.ok(appointmentService.getPendingAppointmentsForDoctorUser(userId));
    }

    @GetMapping("/doctorCompletedAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getCompletedAppointmentsForDoctor(@PathVariable Long userId) {
        return ResponseEntity.ok(appointmentService.getCompletedAppointmentsForDoctorUser(userId));
    }
}
