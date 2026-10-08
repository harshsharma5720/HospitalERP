package com.itmonteur.hospitalerp.appointments.web;

import com.itmonteur.hospitalerp.appointments.internal.FreeSlotDTO;
import com.itmonteur.hospitalerp.appointments.internal.FrontDeskService;
import com.itmonteur.hospitalerp.appointments.internal.WalkInBookingDTO;
import com.itmonteur.hospitalerp.appointments.internal.WalkInBookingRequest;
import com.itmonteur.hospitalerp.patients.PtInfoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Walk-in patients at the front desk (docs/WALK_IN_REGISTRATION_PLAN.md). Under /api/receptionist, so
 * RECEPTIONIST or ADMIN (staff's access rules).
 */
@RestController
@RequestMapping("/api/receptionist")
public class FrontDeskController {

    private final FrontDeskService frontDeskService;

    public FrontDeskController(FrontDeskService frontDeskService) {
        this.frontDeskService = frontDeskService;
    }

    // Patients with this phone number - several for a family sharing one
    @GetMapping("/patients")
    public ResponseEntity<List<PtInfoDTO>> findPatientsByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(frontDeskService.findPatientsByPhone(phone));
    }

    // A doctor's next free slots from now on, in time order
    @GetMapping("/doctors/{doctorId}/next-free-slots")
    public ResponseEntity<List<FreeSlotDTO>> nextFreeSlots(@PathVariable Long doctorId,
                                                           @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(frontDeskService.nextFreeSlots(doctorId, limit));
    }

    // Book for an existing patient, or register a new walk-in patient and book, in one step
    @PostMapping("/walk-in")
    public ResponseEntity<WalkInBookingDTO> registerAndBook(@RequestBody WalkInBookingRequest request) {
        return ResponseEntity.ok(frontDeskService.registerAndBook(request));
    }
}
