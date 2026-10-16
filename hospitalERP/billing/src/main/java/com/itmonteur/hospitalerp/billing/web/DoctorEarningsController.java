package com.itmonteur.hospitalerp.billing.web;

import com.itmonteur.hospitalerp.billing.internal.DoctorEarnings;
import com.itmonteur.hospitalerp.billing.internal.DoctorEarningsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * What a doctor's visits brought in (docs/DOCTOR_ANALYTICS_PLAN.md). Their own under /api/doctor (staff's rules:
 * doctors and admins), any doctor's under /api/admin (administration's rules: admins).
 */
@RestController
public class DoctorEarningsController {

    private final DoctorEarnings doctorEarnings;

    public DoctorEarningsController(DoctorEarnings doctorEarnings) {
        this.doctorEarnings = doctorEarnings;
    }

    // days: 7, 30 or 90
    @GetMapping("/api/doctor/earnings")
    public ResponseEntity<DoctorEarningsDTO> mine(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(doctorEarnings.mine(days));
    }

    @GetMapping("/api/admin/doctors/{doctorId}/earnings")
    public ResponseEntity<DoctorEarningsDTO> forDoctor(@PathVariable long doctorId,
                                                       @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(doctorEarnings.forDoctor(doctorId, days));
    }
}
