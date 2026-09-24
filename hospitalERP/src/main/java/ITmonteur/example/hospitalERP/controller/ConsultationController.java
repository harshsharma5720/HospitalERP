package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.ConsultationDTO;
import ITmonteur.example.hospitalERP.services.ConsultationService;
import ITmonteur.example.hospitalERP.services.PrescriptionPdfService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Consultation notes & prescriptions. Read access is checked per record in ConsultationService.
@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;
    private final PrescriptionPdfService prescriptionPdfService;

    public ConsultationController(ConsultationService consultationService, PrescriptionPdfService prescriptionPdfService) {
        this.consultationService = consultationService;
        this.prescriptionPdfService = prescriptionPdfService;
    }

    // Create or update the consultation of an appointment (marks it completed)
    @PutMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ConsultationDTO> save(@PathVariable long appointmentId,
                                                @Valid @RequestBody ConsultationDTO dto) {
        return ResponseEntity.ok(consultationService.saveConsultation(appointmentId, dto));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ConsultationDTO> get(@PathVariable long appointmentId) {
        return ResponseEntity.ok(consultationService.getByAppointment(appointmentId));
    }

    @GetMapping(value = "/appointment/{appointmentId}/prescription.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> prescriptionPdf(@PathVariable long appointmentId) {
        byte[] pdf = prescriptionPdfService.render(consultationService.getForPrescription(appointmentId));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("prescription-" + appointmentId + ".pdf").build().toString())
                .body(pdf);
    }

    // The logged-in patient's medical history
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<ConsultationDTO>> myHistory() {
        return ResponseEntity.ok(consultationService.getMyHistory());
    }

    // A patient's history, for their doctors and admins
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<ConsultationDTO>> patientHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(consultationService.getPatientHistory(patientId));
    }
}
