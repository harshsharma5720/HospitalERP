package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.services.UserAccountService;
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

    // Patient deletes their own account (or an admin does); path id = the patient's user id
    @DeleteMapping("/api/patient/deleteAccount/{ptId}")
    public ResponseEntity<String> deletePatientAccount(@PathVariable long ptId) {
        userAccountService.deletePatientAccount(ptId);
        return ResponseEntity.ok("Your account has been deleted successfully!!");
    }

    // Path id = doctor.id (not the user id)
    @DeleteMapping("/api/doctor/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDoctorById(@PathVariable Long id) {
        userAccountService.deleteDoctorByDoctorId(id);
        return ResponseEntity.ok("Doctor deleted successfully");
    }

    // Path id = receptionist.id (not the user id)
    @DeleteMapping("/api/receptionist/delete/{receptionistId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteReceptionist(@PathVariable Long receptionistId) {
        userAccountService.deleteReceptionistByReceptionistId(receptionistId);
        return ResponseEntity.ok("Receptionist deleted successfully!");
    }
}
