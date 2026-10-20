package com.itmonteur.hospitalerp.patients.web;

import com.itmonteur.hospitalerp.patients.PtInfoDTO;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import com.itmonteur.hospitalerp.patients.PtRelativeDTO;
import com.itmonteur.hospitalerp.patients.PtRelativeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The front desk's part of a patient's record (docs/ABHA_NUMBER_PLAN.md): the family members, and the ABHA numbers of
 * the patient and of each family member. Under /api/receptionist (staff's rules: receptionists and admins).
 */
@RestController
@RequestMapping("/api/receptionist/patients/{patientId}")
public class PatientDeskController {

    public record AbhaRequest(String abhaNumber) {
    }

    private final PtInfoService ptInfoService;
    private final PtRelativeService ptRelativeService;

    public PatientDeskController(PtInfoService ptInfoService, PtRelativeService ptRelativeService) {
        this.ptInfoService = ptInfoService;
        this.ptRelativeService = ptRelativeService;
    }

    // 14 digits (dashes and spaces allowed), or empty to remove it
    @PutMapping("/abha")
    public ResponseEntity<PtInfoDTO> setAbha(@PathVariable long patientId, @RequestBody AbhaRequest request) {
        return ResponseEntity.ok(ptInfoService.setAbhaNumber(patientId, request.abhaNumber()));
    }

    @GetMapping("/relatives")
    public ResponseEntity<List<PtRelativeDTO>> relatives(@PathVariable long patientId) {
        return ResponseEntity.ok(ptRelativeService.relativesForDesk(patientId));
    }

    @PutMapping("/relatives/{relativeId}/abha")
    public ResponseEntity<PtRelativeDTO> setRelativeAbha(@PathVariable long patientId, @PathVariable long relativeId,
                                                         @RequestBody AbhaRequest request) {
        return ResponseEntity.ok(ptRelativeService.setAbhaNumber(patientId, relativeId, request.abhaNumber()));
    }
}
