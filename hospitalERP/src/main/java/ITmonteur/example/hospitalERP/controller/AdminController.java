package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.dto.DoctorDTO;
import ITmonteur.example.hospitalERP.dto.PtInfoDTO;
import ITmonteur.example.hospitalERP.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private DoctorService doctorService;
    @Autowired
    private JWTService jwtService;
    @Autowired
    private PtInfoService ptInfoService;
    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private ReceptionistService receptionistService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSystemSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDoctors", doctorService.getAllDoctors().size());
        summary.put("totalPatients", ptInfoService.getAllPtInfo().size());
        summary.put("totalAppointments", appointmentService.getAllAppointments().size());
        summary.put("totalReceptionist" , receptionistService.getAllReceptionist().size());
        return ResponseEntity.ok(summary);
    }


}
