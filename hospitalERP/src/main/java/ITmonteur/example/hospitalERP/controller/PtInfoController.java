package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.PtInfoDTO;
import ITmonteur.example.hospitalERP.services.PtInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
public class PtInfoController {

    @Autowired
    private PtInfoService ptInfoService;

    @GetMapping("/getAll")
    public ResponseEntity<List<PtInfoDTO>> getAllAccounts(){
        List<PtInfoDTO> ptInfoDTOs=this.ptInfoService.getAllPtInfo();
        return ResponseEntity.ok(ptInfoDTOs);
    }

    @GetMapping("/getAccount/{ptId}")
    public ResponseEntity<PtInfoDTO> getAccountById(@PathVariable long ptId){
        PtInfoDTO ptInfoDTO = this.ptInfoService.getPtInfoById(ptId);
        return ResponseEntity.ok(ptInfoDTO);
    }
    @DeleteMapping("/deleteAccount/{ptId}")
    public ResponseEntity<String> deleteAccountById(@PathVariable long ptId){
        if (this.ptInfoService.deletePtInfoById(ptId)){
            return ResponseEntity.ok("Your account has been deleted successfully!!");
        }
        else{
            return ResponseEntity.ok("you entered the wrong credentials..");
        }
    }

    @PutMapping("/updateAccount/{ptId}")
    public ResponseEntity<PtInfoDTO> updateAccountById(@RequestBody PtInfoDTO ptInfoDTO,
                                                       @PathVariable long ptId){
        PtInfoDTO  ptInfoDTO1 = this.ptInfoService.updatePtInfoById(ptInfoDTO,ptId);
        return ResponseEntity.ok(ptInfoDTO1);
    }
}
