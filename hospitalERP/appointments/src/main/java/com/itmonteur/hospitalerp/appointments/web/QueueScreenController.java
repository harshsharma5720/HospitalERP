package com.itmonteur.hospitalerp.appointments.web;

import com.itmonteur.hospitalerp.appointments.internal.QueueScreenDTO;
import com.itmonteur.hospitalerp.appointments.internal.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The waiting-room screen's data (docs/TODAYS_QUEUE_PLAN.md): tokens only, no patient data, so no login is needed
 * (AppointmentsSecurityRules).
 */
@RestController
@RequestMapping("/api/queue")
public class QueueScreenController {

    private final QueueService queueService;

    public QueueScreenController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/screen")
    public ResponseEntity<QueueScreenDTO> screen() {
        return ResponseEntity.ok(queueService.screen());
    }
}
