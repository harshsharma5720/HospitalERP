package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.PtRelativeDTO;
import ITmonteur.example.hospitalERP.services.PtRelativeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/patient/relative")
public class PtRelativeController {

    private static final Logger logger = LoggerFactory.getLogger(PtRelativeController.class);

    @Autowired
    private PtRelativeService ptRelativeService;

    @PostMapping("/add")
    public ResponseEntity<PtRelativeDTO> addRelative(@RequestBody PtRelativeDTO dto) {
        logger.info("Request received to add relative for patient ID: {}", dto.getPatientId());
        PtRelativeDTO saved = ptRelativeService.addRelative(dto);
        logger.info("Relative added successfully with ID: {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PtRelativeDTO>> getRelativesByPatient(@PathVariable Long patientId) {
        logger.info("Fetching relatives for patient ID: {}", patientId);
        List<PtRelativeDTO> relatives = ptRelativeService.getRelativesByPatient(patientId);
        logger.info("Total relatives found: {}", relatives.size());
        return ResponseEntity.ok(relatives);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PtRelativeDTO> getRelativeById(@PathVariable Long id) {
        logger.info("Fetching relative with ID: {}", id);
        PtRelativeDTO dto = ptRelativeService.getRelativeById(id);
        logger.info("Relative record retrieved for ID: {}", id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<PtRelativeDTO> updateRelative(@PathVariable Long id, @RequestBody PtRelativeDTO dto) {
        logger.info("Request to update relative with ID: {}", id);
        PtRelativeDTO updated = ptRelativeService.updateRelative(id, dto);
        logger.info("Relative updated successfully for ID: {}", id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteRelative(@PathVariable Long id) {
        logger.info("Request received to delete relative with ID: {}", id);
        String message = ptRelativeService.deleteRelative(id);
        logger.info("Relative deleted with ID: {}", id);
        return ResponseEntity.ok(message);
    }
}
