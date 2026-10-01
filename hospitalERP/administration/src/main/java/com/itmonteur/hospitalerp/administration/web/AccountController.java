package com.itmonteur.hospitalerp.administration.web;

import com.itmonteur.hospitalerp.administration.internal.UserAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Account deletion endpoints. Deleting an account touches every module (profile, bookings,
 * consultations, slots, schedule, leaves, login), so it lives in the administration module.
 * The URLs stay where the frontend expects them; access rules are unchanged:
 * the /api/patient|doctor|receptionist/** rules in SecurityConfig plus the checks below.
 */
@RestController
public class AccountController {

    private final UserAccountService userAccountService;

    public AccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    // These old "delete" URLs deactivate the account now: no login, history kept (docs/ACCOUNT_DEACTIVATION_PLAN.md)

    // Patient deactivates their own account (or an admin does); path id = the patient's user id
    @DeleteMapping("/api/patient/deleteAccount/{ptId}")
    public ResponseEntity<String> deletePatientAccount(@PathVariable long ptId) {
        userAccountService.deactivatePatientAccount(ptId);
        return ResponseEntity.ok("Your account has been deactivated.");
    }

    // Path id = doctor.id (not the user id)
    @DeleteMapping("/api/doctor/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDoctorById(@PathVariable Long id) {
        userAccountService.deactivateDoctorByDoctorId(id);
        return ResponseEntity.ok("Doctor deactivated");
    }

    // Path id = receptionist.id (not the user id)
    @DeleteMapping("/api/receptionist/delete/{receptionistId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteReceptionist(@PathVariable Long receptionistId) {
        userAccountService.deactivateReceptionistByReceptionistId(receptionistId);
        return ResponseEntity.ok("Receptionist deactivated");
    }
}
