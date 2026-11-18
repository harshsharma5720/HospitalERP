package com.itmonteur.hospitalerp.waitlist.web;

import com.itmonteur.hospitalerp.waitlist.internal.JoinRequest;
import com.itmonteur.hospitalerp.waitlist.internal.WaitlistEntryDTO;
import com.itmonteur.hospitalerp.waitlist.internal.WaitlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The logged-in patient's waits (docs/WAITLIST_PLAN.md): for themself or a family member. Patients only. */
@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    // {doctorId, date, shift (MORNING, EVENING or null: either), relativeId?}
    @PostMapping
    public ResponseEntity<WaitlistEntryDTO> join(@RequestBody JoinRequest request) {
        return ResponseEntity.ok(waitlistService.joinAsPatient(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<WaitlistEntryDTO>> mine() {
        return ResponseEntity.ok(waitlistService.mine());
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> leave(@PathVariable long entryId) {
        waitlistService.leave(entryId);
        return ResponseEntity.noContent().build();
    }
}
