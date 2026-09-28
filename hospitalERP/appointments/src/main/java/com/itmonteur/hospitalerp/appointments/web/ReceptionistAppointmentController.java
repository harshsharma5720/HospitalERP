package com.itmonteur.hospitalerp.appointments.web;

import com.itmonteur.hospitalerp.appointments.AppointmentDTO;
import com.itmonteur.hospitalerp.appointments.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Front-desk appointment endpoints. The URLs live under /api/receptionist for the frontend,
 * but the logic belongs to the appointments module. Access: RECEPTIONIST or ADMIN (SecurityConfig).
 */
@RestController
@RequestMapping("/api/receptionist")
public class ReceptionistAppointmentController {

    private final AppointmentService appointmentService;

    public ReceptionistAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // Get all appointments
    @GetMapping("/getAppointments")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    // Get appointments by doctor name
    @GetMapping("/getAppointmentByDoctor/{doctorName}")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointmentsOfDoctor(@PathVariable String doctorName) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByDrName(doctorName));
    }

    // Book on behalf of a patient (ptInfoId and slotId are required)
    @PostMapping("/NewAppointment")
    public ResponseEntity<AppointmentDTO> createNewAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        return ResponseEntity.ok(appointmentService.createAppointment(appointmentDTO));
    }

    @GetMapping("/doctorPendingAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getPendingAppointmentsForDoctor(@PathVariable Long userId) {
        return ResponseEntity.ok(appointmentService.getPendingAppointmentsForDoctorUser(userId));
    }

    @GetMapping("/doctorCompletedAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getCompletedAppointmentsForDoctor(@PathVariable Long userId) {
        return ResponseEntity.ok(appointmentService.getCompletedAppointmentsForDoctorUser(userId));
    }

    // Cancel an appointment (kept in history with a CANCELLED status)
    @DeleteMapping("/deleteAppointment/{appointmentId}")
    public ResponseEntity<String> deleteAppointment(@PathVariable Long appointmentId) {
        appointmentService.cancelAppointment(appointmentId);
        return ResponseEntity.ok("Appointment cancelled successfully!");
    }
}
