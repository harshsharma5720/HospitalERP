package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.DoctorDTO;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.services.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @PostMapping("/addNewDoctor")
    public ResponseEntity<DoctorDTO> addDoctor(@RequestBody DoctorDTO doctorDTO) {
        DoctorDTO savedDoctor = doctorService.registerDoctor(doctorDTO);
        return ResponseEntity.ok(savedDoctor);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<DoctorDTO>> getAllDoctors() {
        List<DoctorDTO> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(doctors);
    }
    @GetMapping("/get/{id}")
    public ResponseEntity<DoctorDTO> getDoctorById(@PathVariable Long id) {
        DoctorDTO doctorDTO = doctorService.getDoctorById(id);
        return ResponseEntity.ok(doctorDTO);
    }

//    @GetMapping("/get/{id}")
//    public ResponseEntity<DoctorDTO> getDoctorById(@PathVariable Long id) {
//        return doctorService.getDoctorById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
    @PutMapping("/update/{id}")
    public ResponseEntity<DoctorDTO> updateDoctor(@PathVariable Long id,
                                                  @RequestBody DoctorDTO doctorDTO) {
        DoctorDTO updatedDoctor = doctorService.updateDoctor(id, doctorDTO);
        return ResponseEntity.ok(updatedDoctor);
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteDoctorById(@PathVariable Long id) {
        boolean deleted = doctorService.deleteDoctor(id);
        if (deleted) {
            return ResponseEntity.ok("Doctor deleted successfully");
        } else {
            return ResponseEntity.status(404).body("Doctor not found");
        }
    }
    @DeleteMapping
    public ResponseEntity<String> deleteAllDoctors() {
        boolean deleted = doctorService.deleteAllDoctors();
        if (deleted) {
            return ResponseEntity.ok("All doctors deleted successfully");
        } else {
            return ResponseEntity.status(404).body("No doctors found to delete");
        }
    }

}
