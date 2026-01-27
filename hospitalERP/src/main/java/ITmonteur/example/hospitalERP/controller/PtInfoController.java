package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.DoctorDTO;
import ITmonteur.example.hospitalERP.dto.PtInfoDTO;
import ITmonteur.example.hospitalERP.entities.Specialist;
import ITmonteur.example.hospitalERP.services.DoctorService;
import ITmonteur.example.hospitalERP.services.PtInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/patient")
public class PtInfoController {

    private static final Logger logger = LoggerFactory.getLogger(PtInfoController.class);

    @Autowired
    private PtInfoService ptInfoService;

    @Autowired
    private DoctorService doctorService;

    // Get all patient accounts
    @GetMapping("/getAll")
    public ResponseEntity<List<PtInfoDTO>> getAllAccounts(){
        logger.info("Fetching all patient accounts");
        List<PtInfoDTO> ptInfoDTOs = this.ptInfoService.getAllPtInfo();
        logger.info("Total patient accounts fetched: {}", ptInfoDTOs.size());
        return ResponseEntity.ok(ptInfoDTOs);
    }

    // Get account by patient ID
    @GetMapping("/getAccount/{ptId}")
    public ResponseEntity<PtInfoDTO> getAccountById(@PathVariable long ptId){
        logger.info("Fetching patient account with ID: {}", ptId);
        PtInfoDTO ptInfoDTO = this.ptInfoService.getPtInfoById(ptId);
        logger.info("Fetched account for patient: {}", ptInfoDTO.getPatientName());
        return ResponseEntity.ok(ptInfoDTO);
    }
    @GetMapping("/getAllDoctors")
//    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'RECEPTIONIST')")
    public ResponseEntity<List<DoctorDTO>> getAllDoctors() {
        logger.info("Fetching all doctors");
        List<DoctorDTO> doctors = this.doctorService.getAllDoctors();
        logger.info("Total doctors fetched: {}", doctors.size());
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/getAllBySpecialization")
    public ResponseEntity<List<DoctorDTO>> getDoctorsBySpecialization(@RequestParam String specialization){
        logger.info("Fetching all doctors od specialization:" +specialization);
        Specialist specEnum = Specialist.valueOf(specialization.toUpperCase());
        List<DoctorDTO> doctorDTOS = ptInfoService.findDoctorsBySpecialization(specEnum);
        logger.info("Total doctors fetched: {}", doctorDTOS.size());
        return ResponseEntity.ok(doctorDTOS);
    }

    // Delete account by patient ID
    @DeleteMapping("/deleteAccount/{ptId}")
    public ResponseEntity<String> deleteAccountById(@PathVariable long ptId){
        logger.info("Deleting patient account with ID: {}", ptId);
        if (this.ptInfoService.deletePtInfoById(ptId)){
            logger.info("Patient account deleted successfully with ID: {}", ptId);
            return ResponseEntity.ok("Your account has been deleted successfully!!");
        } else {
            logger.warn("Failed to delete patient account with ID: {}", ptId);
            return ResponseEntity.ok("You entered the wrong credentials..");
        }
    }

    // Update patient account by ID
    @PutMapping(value = "/updateAccount/{ptId}", consumes = {"multipart/form-data"})
    public ResponseEntity<PtInfoDTO> updateAccountById(
            @PathVariable long ptId,
            @RequestPart("ptInfoDTO") PtInfoDTO ptInfoDTO,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {

        logger.info("Updating patient account with ID: {}", ptId);
        // Handle file upload
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String uploadDir = System.getProperty("user.dir") + "/uploads/profileImages/";
                java.nio.file.Path filePath = java.nio.file.Paths.get(uploadDir + profileImage.getOriginalFilename());
                java.nio.file.Files.createDirectories(filePath.getParent());
                profileImage.transferTo(filePath.toFile());
                ptInfoDTO.setProfileImage("/uploads/profileImages/" + profileImage.getOriginalFilename());
            } catch (Exception e) {
                logger.error("Failed to upload profile image for patient ID {}: {}", ptId, e.getMessage());
            }
        }
        PtInfoDTO updatedPtInfoDTO = this.ptInfoService.updatePtInfoById(ptInfoDTO, ptId);
        logger.info("Patient account updated successfully for ID: {}", ptId);
        return ResponseEntity.ok(updatedPtInfoDTO);
    }
}
