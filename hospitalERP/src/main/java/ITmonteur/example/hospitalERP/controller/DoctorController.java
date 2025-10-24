package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.DoctorDTO;
import ITmonteur.example.hospitalERP.entities.Specialist;
import ITmonteur.example.hospitalERP.services.DoctorService;
import ITmonteur.example.hospitalERP.services.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorController.class);

    @Autowired
    private DoctorService doctorService;
    @Autowired
    private JWTService jwtService;

    // Add new doctor
    @PostMapping("/addNewDoctor")
    public ResponseEntity<DoctorDTO> addDoctor(@RequestBody DoctorDTO doctorDTO) {
        logger.info("Adding new doctor: {}", doctorDTO.getName());
        DoctorDTO savedDoctor = doctorService.registerDoctor(doctorDTO);
        logger.info("Doctor added successfully with ID: {}", savedDoctor.getId());
        return ResponseEntity.ok(savedDoctor);
    }

    // Get all doctors
    @GetMapping("/getAll")
    public ResponseEntity<List<DoctorDTO>> getAllDoctors() {
        logger.info("Fetching all doctors");
        List<DoctorDTO> doctors = doctorService.getAllDoctors();
        logger.info("Total doctors fetched: {}", doctors.size());
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/getAllBySpecialization")
    public ResponseEntity<List<DoctorDTO>> getDoctorsBySpecialization(@RequestBody String specialisation){
        try {
            logger.info("Fetching all doctors od specialization:" +specialisation);
            Specialist specEnum = Specialist.valueOf(specialisation.toUpperCase());
            List<DoctorDTO> doctorDTOS = doctorService.findDoctorsBySpecialization(specEnum);
            logger.info("Total doctors fetched: {}", doctorDTOS.size());
            return ResponseEntity.ok(doctorDTOS);
        }catch (IllegalArgumentException e) {
            return ResponseEntity.ok(List.of());
        }
    }

    // Get doctor by ID
    @GetMapping("/get/{id}")
    public ResponseEntity<DoctorDTO> getDoctorById(@PathVariable Long id) {
        logger.info("Fetching doctor with ID: {}", id);
        DoctorDTO doctorDTO = doctorService.getDoctorById(id);
        logger.info("Fetched doctor: {}", doctorDTO.getName());
        return ResponseEntity.ok(doctorDTO);
    }

    // Update doctor by ID (only self-update allowed)
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable Long id,
                                          @RequestBody DoctorDTO doctorDTO,
                                          HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String username = jwtService.extractUsername(token);
        Long tokenUserId = jwtService.extractUserId(token);
        logger.info("Update request by user: {} for doctor ID: {}", username, id);
        // Check if the doctor is updating own profile
        if (!id.equals(tokenUserId)) {
            logger.warn("User {} tried to update doctor ID {} without permission", username, id);
            return ResponseEntity.status(403).body("You can only update your own profile!");
        }
        DoctorDTO updatedDoctor = doctorService.updateDoctor(id, doctorDTO);
        logger.info("Doctor updated successfully with ID: {}", id);
        return ResponseEntity.ok(updatedDoctor);
    }

    // Delete doctor by ID
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteDoctorById(@PathVariable Long id) {
        logger.info("Deleting doctor with ID: {}", id);
        boolean deleted = doctorService.deleteDoctor(id);
        if (deleted) {
            logger.info("Doctor deleted successfully with ID: {}", id);
            return ResponseEntity.ok("Doctor deleted successfully");
        } else {
            logger.warn("Doctor not found with ID: {}", id);
            return ResponseEntity.status(404).body("Doctor not found");
        }
    }

    // Delete all doctors
    @DeleteMapping
    public ResponseEntity<String> deleteAllDoctors() {
        logger.info("Deleting all doctors");
        boolean deleted = doctorService.deleteAllDoctors();
        if (deleted) {
            logger.info("All doctors deleted successfully");
            return ResponseEntity.ok("All doctors deleted successfully");
        } else {
            logger.warn("No doctors found to delete");
            return ResponseEntity.status(404).body("No doctors found to delete");
        }
    }
}
