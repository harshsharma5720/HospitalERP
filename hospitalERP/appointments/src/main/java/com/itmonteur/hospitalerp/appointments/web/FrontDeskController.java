package com.itmonteur.hospitalerp.appointments.web;

import com.itmonteur.hospitalerp.appointments.AppointmentDTO;
import com.itmonteur.hospitalerp.appointments.internal.AppointmentListQuery;
import com.itmonteur.hospitalerp.appointments.internal.FreeSlotDTO;
import com.itmonteur.hospitalerp.appointments.internal.FrontDeskService;
import com.itmonteur.hospitalerp.appointments.internal.WalkInBookingDTO;
import com.itmonteur.hospitalerp.appointments.internal.WalkInBookingRequest;
import com.itmonteur.hospitalerp.common.PageResult;
import com.itmonteur.hospitalerp.patients.PtInfoDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * The front desk: walk-in patients (docs/WALK_IN_REGISTRATION_PLAN.md), patient search and the appointment list
 * (docs/PATIENT_SEARCH_PLAN.md). Under /api/receptionist, so RECEPTIONIST or ADMIN (staff's access rules).
 */
@RestController
@RequestMapping("/api/receptionist")
public class FrontDeskController {

    private final FrontDeskService frontDeskService;

    public FrontDeskController(FrontDeskService frontDeskService) {
        this.frontDeskService = frontDeskService;
    }

    // Patients with this phone number - several for a family sharing one
    @GetMapping("/patients")
    public ResponseEntity<List<PtInfoDTO>> findPatientsByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(frontDeskService.findPatientsByPhone(phone));
    }

    // The one search box: part of a name, a phone number or a patient ID ("#41"); empty lists everyone
    @GetMapping("/patients/search")
    public ResponseEntity<PageResult<PtInfoDTO>> searchPatients(@RequestParam(required = false) String q,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(frontDeskService.searchPatients(q, page, size));
    }

    // One patient's details
    @GetMapping("/patients/{patientId}")
    public ResponseEntity<PtInfoDTO> patient(@PathVariable long patientId) {
        return ResponseEntity.ok(frontDeskService.patient(patientId));
    }

    // Appointments by date and time; every filter optional
    @GetMapping("/appointments")
    public ResponseEntity<PageResult<AppointmentDTO>> appointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(frontDeskService.listAppointments(
                new AppointmentListQuery(from, to, doctorId, status, patient, patientId, order), page, size));
    }

    // A doctor's next free slots from now on, in time order
    @GetMapping("/doctors/{doctorId}/next-free-slots")
    public ResponseEntity<List<FreeSlotDTO>> nextFreeSlots(@PathVariable Long doctorId,
                                                           @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(frontDeskService.nextFreeSlots(doctorId, limit));
    }

    // Book for an existing patient, or register a new walk-in patient and book, in one step
    @PostMapping("/walk-in")
    public ResponseEntity<WalkInBookingDTO> registerAndBook(@RequestBody WalkInBookingRequest request) {
        return ResponseEntity.ok(frontDeskService.registerAndBook(request));
    }
}
