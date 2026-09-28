package com.itmonteur.hospitalerp.staff.web;

import com.itmonteur.hospitalerp.staff.DoctorDTO;
import com.itmonteur.hospitalerp.staff.Specialist;
import com.itmonteur.hospitalerp.staff.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public doctor directory used by the patient pages. The URLs live under /api/patient for the
 * frontend, but the data belongs to the staff module. Public (permitAll in SecurityConfig).
 */
@RestController
@RequestMapping("/api/patient")
public class DoctorDirectoryController {

    private final DoctorService doctorService;

    public DoctorDirectoryController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/getAllDoctors")
    public ResponseEntity<List<DoctorDTO>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    // Unknown specializations return an empty list
    @GetMapping("/getAllBySpecialization")
    public ResponseEntity<List<DoctorDTO>> getDoctorsBySpecialization(@RequestParam String specialization) {
        Specialist specialist = DoctorService.parseSpecialist(specialization);
        return ResponseEntity.ok(specialist == null ? List.of() : doctorService.findDoctorsBySpecialization(specialist));
    }
}
