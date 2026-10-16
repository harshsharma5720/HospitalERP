package com.itmonteur.hospitalerp.appointments.web;

import com.itmonteur.hospitalerp.appointments.DoctorAnalytics;
import com.itmonteur.hospitalerp.appointments.DoctorAnalyticsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A doctor's visits in numbers (docs/DOCTOR_ANALYTICS_PLAN.md). Their own under /api/doctor (staff's rules: doctors
 * and admins), any doctor's under /api/admin (administration's rules: admins).
 */
@RestController
public class DoctorAnalyticsController {

    private final DoctorAnalytics doctorAnalytics;

    public DoctorAnalyticsController(DoctorAnalytics doctorAnalytics) {
        this.doctorAnalytics = doctorAnalytics;
    }

    // days: 7, 30 or 90
    @GetMapping("/api/doctor/analytics")
    public ResponseEntity<DoctorAnalyticsDTO> mine(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(doctorAnalytics.mine(days));
    }

    @GetMapping("/api/admin/doctors/{doctorId}/analytics")
    public ResponseEntity<DoctorAnalyticsDTO> forDoctor(@PathVariable long doctorId,
                                                        @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(doctorAnalytics.forDoctor(doctorId, days));
    }
}
