package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionPdfServiceTest {

    @Test
    void rendersAValidPdf() {
        Doctor doctor = new Doctor();
        doctor.setName("Rao");
        doctor.setSpecialist(Specialist.CARDIOLOGY);
        Appointment appointment = new Appointment();
        appointment.setAppointmentID(12);
        appointment.setDoctor(doctor);
        appointment.setPatientName("Asha <script>");
        appointment.setAge(30);
        appointment.setGender(Gender.FEMALE);
        appointment.setDate(LocalDate.of(2026, 3, 10));
        Consultation consultation = new Consultation();
        consultation.setAppointment(appointment);
        consultation.setDiagnosis("Hypertension");
        consultation.setBloodPressure("150/95");
        consultation.setFollowUpDate(LocalDate.of(2026, 4, 10));
        PrescriptionItem item = new PrescriptionItem();
        item.setMedicineName("Amlodipine");
        item.setDosage("5 mg");
        consultation.replaceMedicines(List.of(item));

        byte[] pdf = new PrescriptionPdfService("City Hospital", "MG Road", "0123").render(consultation);

        assertThat(pdf.length).isGreaterThan(1000);
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
