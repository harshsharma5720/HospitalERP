package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.DoctorScheduleDTO;
import ITmonteur.example.hospitalERP.services.DoctorScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * A doctor's weekly working hours. The URL lives under /api/doctor for the frontend,
 * but the logic belongs to the scheduling module. Access: DOCTOR or ADMIN (SecurityConfig);
 * a doctor can only read/change their own schedule (DoctorScheduleService).
 */
@RestController
@RequestMapping("/api/doctor")
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    public DoctorScheduleController(DoctorScheduleService doctorScheduleService) {
        this.doctorScheduleService = doctorScheduleService;
    }

    // userId = the doctor's user id
    @GetMapping("/{userId}/schedule")
    public ResponseEntity<List<DoctorScheduleDTO>> getSchedule(@PathVariable Long userId) {
        return ResponseEntity.ok(doctorScheduleService.getSchedule(userId));
    }

    @PutMapping("/{userId}/schedule")
    public ResponseEntity<List<DoctorScheduleDTO>> updateSchedule(@PathVariable Long userId,
                                                                  @Valid @RequestBody List<@Valid DoctorScheduleDTO> schedule) {
        return ResponseEntity.ok(doctorScheduleService.updateSchedule(userId, schedule));
    }
}
