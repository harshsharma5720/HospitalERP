package com.itmonteur.hospitalerp.appointments.web;

import com.itmonteur.hospitalerp.appointments.AppointmentDTO;
import com.itmonteur.hospitalerp.appointments.internal.DoctorQueueDTO;
import com.itmonteur.hospitalerp.appointments.internal.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The logged-in doctor's queue today (docs/TODAYS_QUEUE_PLAN.md). Under /api/doctor (staff's access rules); only
 * doctors have a queue, so admins are refused too.
 */
@RestController
@RequestMapping("/api/doctor/queue")
public class DoctorQueueController {

    private final QueueService queueService;

    public DoctorQueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    // With the doctor, waiting (by time), not arrived, done
    @GetMapping
    public ResponseEntity<DoctorQueueDTO> myQueue() {
        return ResponseEntity.ok(queueService.myQueue());
    }

    // The first waiting patient by appointment time (409 when nobody is waiting)
    @PostMapping("/next")
    public ResponseEntity<AppointmentDTO> callNext() {
        return ResponseEntity.ok(queueService.callNext());
    }

    // This waiting patient, e.g. an urgent one
    @PostMapping("/{appointmentId}/call")
    public ResponseEntity<AppointmentDTO> call(@PathVariable long appointmentId) {
        return ResponseEntity.ok(queueService.call(appointmentId));
    }

    // Called in but not there: back to waiting
    @PostMapping("/{appointmentId}/not-here")
    public ResponseEntity<AppointmentDTO> notHere(@PathVariable long appointmentId) {
        return ResponseEntity.ok(queueService.notHere(appointmentId));
    }
}
