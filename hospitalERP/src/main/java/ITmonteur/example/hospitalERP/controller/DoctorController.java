package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.dto.DoctorDTO;
import ITmonteur.example.hospitalERP.dto.DoctorScheduleDTO;
import ITmonteur.example.hospitalERP.entities.Specialist;
import ITmonteur.example.hospitalERP.services.DoctorScheduleService;
import ITmonteur.example.hospitalERP.services.DoctorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// getAll / getAllBySpecialization / getDoctor/{id} are public; the rest needs DOCTOR or ADMIN
@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;
    @Autowired
    private DoctorScheduleService doctorScheduleService;

    // Get all doctors
    @GetMapping("/getAll")
    public ResponseEntity<List<DoctorDTO>> getAllDoctors() {
        return ResponseEntity.ok(this.doctorService.getAllDoctors());
    }

    @GetMapping("/getAllBySpecialization")
    public ResponseEntity<List<DoctorDTO>> getDoctorsBySpecialization(@RequestParam String specialisation) {
        Specialist specialist = DoctorService.parseSpecialist(specialisation);
        return ResponseEntity.ok(specialist == null ? List.of() : this.doctorService.findDoctorsBySpecialization(specialist));
    }

    // Get doctor by user ID (self or admin)
    @GetMapping("/get/{id}")
    public ResponseEntity<DoctorDTO> getDoctorByUserId(@PathVariable Long id) {
        return ResponseEntity.ok(this.doctorService.getDoctorByUserId(id));
    }

    // Get doctor by doctor ID (public profile)
    @GetMapping("/getDoctor/{id}")
    public ResponseEntity<DoctorDTO> getDoctorByDoctorId(@PathVariable Long id) {
        return ResponseEntity.ok(this.doctorService.getDoctorByDoctorId(id));
    }

    @PutMapping("/complete/{appointmentId}")
    public ResponseEntity<String> markAppointmentCompleted(@PathVariable long appointmentId) {
        return ResponseEntity.ok(this.doctorService.markAsCompleted(appointmentId));
    }

    @GetMapping("/doctorPendingAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getPendingAppointmentsForDoctor(@PathVariable Long userId) {
        return ResponseEntity.ok(this.doctorService.getAllPendingAppointmentsByDoctorId(userId));
    }

    @GetMapping("/doctorCompletedAppointments/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getCompletedAppointmentsForDoctor(@PathVariable Long userId) {
        return ResponseEntity.ok(this.doctorService.getAllCompletedAppointmentsByDoctorId(userId));
    }

    // Update doctor by user ID (self or admin)
    @PutMapping(value = "/update/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<DoctorDTO> updateDoctor(
            @PathVariable Long id,
            @RequestPart("doctorDTO") DoctorDTO doctorDTO,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        return ResponseEntity.ok(this.doctorService.updateDoctor(id, doctorDTO, profileImage));
    }

    // Weekly working hours (self or admin); id = the doctor's user id
    @GetMapping("/{userId}/schedule")
    public ResponseEntity<List<DoctorScheduleDTO>> getSchedule(@PathVariable Long userId) {
        return ResponseEntity.ok(this.doctorScheduleService.getSchedule(userId));
    }

    @PutMapping("/{userId}/schedule")
    public ResponseEntity<List<DoctorScheduleDTO>> updateSchedule(@PathVariable Long userId,
                                                                  @Valid @RequestBody List<@Valid DoctorScheduleDTO> schedule) {
        return ResponseEntity.ok(this.doctorScheduleService.updateSchedule(userId, schedule));
    }

    // Delete doctor by doctor ID
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDoctorById(@PathVariable Long id) {
        this.doctorService.deleteDoctor(id);
        return ResponseEntity.ok("Doctor deleted successfully");
    }
}
