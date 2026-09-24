package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.DoctorDTO;
import ITmonteur.example.hospitalERP.dto.PtInfoDTO;
import ITmonteur.example.hospitalERP.entities.Specialist;
import ITmonteur.example.hospitalERP.services.DoctorService;
import ITmonteur.example.hospitalERP.services.PtInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Path IDs ("ptId") are the patient's user id. Ownership is checked in PtInfoService.
@RestController
@RequestMapping("/api/patient")
public class PtInfoController {

    @Autowired
    private PtInfoService ptInfoService;

    @Autowired
    private DoctorService doctorService;

    // Get all patient accounts (admin / receptionist)
    @GetMapping("/getAll")
    public ResponseEntity<List<PtInfoDTO>> getAllAccounts(){
        return ResponseEntity.ok(this.ptInfoService.getAllPtInfo());
    }

    // Get account by user ID
    @GetMapping("/getAccount/{ptId}")
    public ResponseEntity<PtInfoDTO> getAccountById(@PathVariable long ptId){
        return ResponseEntity.ok(this.ptInfoService.getPtInfoById(ptId));
    }

    // Public doctor directory
    @GetMapping("/getAllDoctors")
    public ResponseEntity<List<DoctorDTO>> getAllDoctors() {
        return ResponseEntity.ok(this.doctorService.getAllDoctors());
    }

    @GetMapping("/getAllBySpecialization")
    public ResponseEntity<List<DoctorDTO>> getDoctorsBySpecialization(@RequestParam String specialization){
        Specialist specialist = DoctorService.parseSpecialist(specialization);
        return ResponseEntity.ok(specialist == null ? List.of() : doctorService.findDoctorsBySpecialization(specialist));
    }

    // Delete account by user ID
    @DeleteMapping("/deleteAccount/{ptId}")
    public ResponseEntity<String> deleteAccountById(@PathVariable long ptId){
        this.ptInfoService.deletePtInfoById(ptId);
        return ResponseEntity.ok("Your account has been deleted successfully!!");
    }

    // Update patient account by user ID
    @PutMapping(value = "/updateAccount/{ptId}", consumes = {"multipart/form-data"})
    public ResponseEntity<PtInfoDTO> updateAccountById(
            @PathVariable long ptId,
            @RequestPart("ptInfoDTO") PtInfoDTO ptInfoDTO,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        return ResponseEntity.ok(this.ptInfoService.updatePtInfoById(ptInfoDTO, ptId, profileImage));
    }
}
