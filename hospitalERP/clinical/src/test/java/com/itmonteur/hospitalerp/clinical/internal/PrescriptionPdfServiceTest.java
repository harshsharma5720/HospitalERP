package com.itmonteur.hospitalerp.clinical.internal;

import com.itmonteur.hospitalerp.common.Gender;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.Specialist;
import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.clinical.Consultation;
import com.itmonteur.hospitalerp.clinical.PrescriptionItem;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

    @Test
    void theABHANumberIsUnderThePatientsDetails() throws IOException {
        PtInfo patient = new PtInfo();
        patient.setPatientName("Asha Rao");
        patient.setAbhaNumber("91000000000001");
        Appointment appointment = new Appointment();
        appointment.setAppointmentID(13);
        appointment.setPtInfo(patient);
        appointment.setPatientName("Asha Rao");
        appointment.setAge(30);
        appointment.setGender(Gender.FEMALE);
        appointment.setDate(LocalDate.of(2026, 3, 10));
        Consultation consultation = new Consultation();
        consultation.setAppointment(appointment);
        consultation.setDiagnosis("Hypertension");
        PrescriptionPdfService service = new PrescriptionPdfService("City Hospital", "MG Road", "0123");

        assertThat(text(service.render(consultation))).containsPattern("Asha Rao\\s+30 yrs, Female\\s+ABHA 91-0000-0000-0001");
        appointment.setPatientName("Ravi Rao");
        assertThat(text(service.render(consultation))).as("someone else's visit").doesNotContain("ABHA");
    }

    private static String text(byte[] document) throws IOException {
        PdfReader reader = new PdfReader(document);
        StringBuilder text = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        return text.toString();
    }
}
