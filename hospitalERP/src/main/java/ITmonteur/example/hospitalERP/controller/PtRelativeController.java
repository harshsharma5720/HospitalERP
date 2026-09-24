package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.PtRelativeDTO;
import ITmonteur.example.hospitalERP.services.PtRelativeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Ownership is checked in PtRelativeService
@RestController
@RequestMapping("/api/patient/relative")
public class PtRelativeController {

    @Autowired
    private PtRelativeService ptRelativeService;

    @PostMapping("/add")
    public ResponseEntity<PtRelativeDTO> addRelative(@Valid @RequestBody PtRelativeDTO dto) {
        return ResponseEntity.ok(ptRelativeService.addRelative(dto));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PtRelativeDTO>> getRelativesByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(ptRelativeService.getRelativesByPatient(patientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PtRelativeDTO> getRelativeById(@PathVariable Long id) {
        return ResponseEntity.ok(ptRelativeService.getRelativeById(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<PtRelativeDTO> updateRelative(@PathVariable Long id, @Valid @RequestBody PtRelativeDTO dto) {
        return ResponseEntity.ok(ptRelativeService.updateRelative(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteRelative(@PathVariable Long id) {
        return ResponseEntity.ok(ptRelativeService.deleteRelative(id));
    }
}
