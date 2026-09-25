package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.PtInfoDTO;
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

    // Update patient account by user ID
    @PutMapping(value = "/updateAccount/{ptId}", consumes = {"multipart/form-data"})
    public ResponseEntity<PtInfoDTO> updateAccountById(
            @PathVariable long ptId,
            @RequestPart("ptInfoDTO") PtInfoDTO ptInfoDTO,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        return ResponseEntity.ok(this.ptInfoService.updatePtInfoById(ptInfoDTO, ptId, profileImage));
    }
}
