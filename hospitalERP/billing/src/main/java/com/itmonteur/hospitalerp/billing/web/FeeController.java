package com.itmonteur.hospitalerp.billing.web;

import com.itmonteur.hospitalerp.billing.internal.DoctorFeeDTO;
import com.itmonteur.hospitalerp.billing.internal.FeeListDTO;
import com.itmonteur.hospitalerp.billing.internal.FeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Consultation fees per doctor (docs/BILLING_PLAN.md). Under /api/admin, so admins only (administration's access
 * rules).
 */
@RestController
@RequestMapping("/api/admin/fees")
public class FeeController {

    /** The body of PUT /api/admin/fees/{doctorId}: {"consultationFee": 500} */
    public record FeeRequest(BigDecimal consultationFee) {
    }

    private final FeeService feeService;

    public FeeController(FeeService feeService) {
        this.feeService = feeService;
    }

    // Every doctor with their fee, and the default fee for doctors without one
    @GetMapping
    public ResponseEntity<FeeListDTO> listFees() {
        return ResponseEntity.ok(feeService.listFees());
    }

    @PutMapping("/{doctorId}")
    public ResponseEntity<DoctorFeeDTO> setFee(@PathVariable long doctorId, @RequestBody FeeRequest request) {
        return ResponseEntity.ok(feeService.setFee(doctorId, request.consultationFee()));
    }
}
