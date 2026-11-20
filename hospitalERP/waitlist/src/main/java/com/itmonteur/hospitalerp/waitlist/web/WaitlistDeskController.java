package com.itmonteur.hospitalerp.waitlist.web;

import com.itmonteur.hospitalerp.waitlist.internal.JoinRequest;
import com.itmonteur.hospitalerp.waitlist.internal.WaitlistEntryDTO;
import com.itmonteur.hospitalerp.waitlist.internal.WaitlistService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * The front desk's waitlist (docs/WAITLIST_PLAN.md): a day's waits, putting a patient or a family member on one,
 * removing one. Under /api/receptionist (staff's rules: receptionists and admins).
 */
@RestController
@RequestMapping("/api/receptionist/waitlist")
public class WaitlistDeskController {

    private final WaitlistService waitlistService;

    public WaitlistDeskController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    // date: today when not given; doctorId: all doctors when not given
    @GetMapping
    public ResponseEntity<List<WaitlistEntryDTO>> day(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long doctorId) {
        return ResponseEntity.ok(waitlistService.forDay(date, doctorId));
    }

    // {patientId, relativeId?, doctorId, date, shift (MORNING, EVENING or null: either)}
    @PostMapping
    public ResponseEntity<WaitlistEntryDTO> add(@RequestBody JoinRequest request) {
        return ResponseEntity.ok(waitlistService.joinForPatient(request));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> remove(@PathVariable long entryId) {
        waitlistService.remove(entryId);
        return ResponseEntity.noContent().build();
    }
}
