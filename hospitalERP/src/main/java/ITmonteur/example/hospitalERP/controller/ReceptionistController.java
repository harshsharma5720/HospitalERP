package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.ReceptionistDTO;
import ITmonteur.example.hospitalERP.services.ReceptionistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Everything under /api/receptionist requires RECEPTIONIST or ADMIN (see SecurityConfig)
@RestController
@RequestMapping("/api/receptionist")
public class ReceptionistController {

    @Autowired
    private ReceptionistService receptionistService;

    // Get all receptionists
    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReceptionistDTO>> getAllReceptionist() {
        return ResponseEntity.ok(this.receptionistService.getAllReceptionist());
    }

    // Get receptionist by user ID (self or admin)
    @GetMapping("/getReceptionist/{receptionistId}")
    public ResponseEntity<ReceptionistDTO> getReceptionistByID(@PathVariable long receptionistId) {
        return ResponseEntity.ok(this.receptionistService.getReceptionistByID(receptionistId));
    }

    // Update receptionist by user ID (self or admin)
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ReceptionistDTO> updateReceptionist(
            @PathVariable("id") Long id,
            @RequestPart("receptionistDTO") ReceptionistDTO receptionistDTO,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        return ResponseEntity.ok(this.receptionistService.updateReceptionist(id, receptionistDTO, profileImage));
    }
}
