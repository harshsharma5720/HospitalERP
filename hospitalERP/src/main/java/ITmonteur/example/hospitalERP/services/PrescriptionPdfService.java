package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renders a consultation as an A4 prescription PDF (OpenPDF). */
@Service
public class PrescriptionPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final Color BRAND = new Color(30, 99, 219);
    private static final Color LIGHT = new Color(235, 242, 255);

    private final String hospitalName;
    private final String hospitalAddress;
    private final String hospitalPhone;

    public PrescriptionPdfService(@Value("${app.hospital.name:Hospital ERP}") String hospitalName,
                                  @Value("${app.hospital.address:}") String hospitalAddress,
                                  @Value("${app.hospital.phone:}") String hospitalPhone) {
        this.hospitalName = hospitalName;
        this.hospitalAddress = hospitalAddress;
        this.hospitalPhone = hospitalPhone;
    }

    public byte[] render(Consultation consultation) {
        Appointment appointment = consultation.getAppointment();
        Doctor doctor = appointment.getDoctor();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, out);
        document.open();

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND);
        Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BRAND);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        // Letterhead
        Paragraph hospital = new Paragraph(hospitalName, title);
        document.add(hospital);
        List<String> contact = new ArrayList<>();
        if (!hospitalAddress.isBlank()) contact.add(hospitalAddress);
        if (!hospitalPhone.isBlank()) contact.add("Phone: " + hospitalPhone);
        if (!contact.isEmpty()) {
            document.add(new Paragraph(String.join("  |  ", contact), small));
        }
        document.add(new Paragraph(" "));

        // Doctor / patient block
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.addCell(block("Doctor",
                "Dr. " + (doctor != null ? doctor.getName() : "-")
                        + (doctor != null && doctor.getSpecialist() != null && doctor.getSpecialist() != Specialist.NOT_ASSIGNED
                        ? "\n" + pretty(doctor.getSpecialist().name()) : ""), bold, normal));
        String patientLine = appointment.getPatientName()
                + "\n" + (appointment.getAge() > 0 ? appointment.getAge() + " yrs, " : "")
                + (appointment.getGender() != null ? pretty(appointment.getGender().name()) : "");
        info.addCell(block("Patient", patientLine, bold, normal));
        info.addCell(block("Date", appointment.getDate().format(DATE), bold, normal));
        info.addCell(block("Appointment #", String.valueOf(appointment.getAppointmentID()), bold, normal));
        document.add(info);

        // Vitals
        List<String> vitals = new ArrayList<>();
        if (consultation.getBloodPressure() != null) vitals.add("BP " + consultation.getBloodPressure() + " mmHg");
        if (consultation.getPulse() != null) vitals.add("Pulse " + consultation.getPulse() + " bpm");
        if (consultation.getTemperature() != null) vitals.add("Temp " + consultation.getTemperature() + " °C");
        if (consultation.getWeightKg() != null) vitals.add("Weight " + consultation.getWeightKg() + " kg");
        if (!vitals.isEmpty()) {
            section(document, "Vitals", String.join("   •   ", vitals), heading, normal);
        }
        if (consultation.getSymptoms() != null) {
            section(document, "Symptoms", consultation.getSymptoms(), heading, normal);
        }
        section(document, "Diagnosis", consultation.getDiagnosis(), heading, normal);

        // Rx table
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Rx", FontFactory.getFont(FontFactory.TIMES_BOLDITALIC, 20, BRAND)));
        if (consultation.getMedicines().isEmpty()) {
            document.add(new Paragraph("No medicines prescribed.", normal));
        } else {
            PdfPTable rx = new PdfPTable(new float[]{0.5f, 3f, 1.5f, 1.8f, 1.4f, 2.5f});
            rx.setWidthPercentage(100);
            rx.setSpacingBefore(4);
            for (String header : new String[]{"#", "Medicine", "Dosage", "Frequency", "Duration", "Instructions"}) {
                PdfPCell cell = new PdfPCell(new Phrase(header, bold));
                cell.setBackgroundColor(LIGHT);
                cell.setPadding(5);
                rx.addCell(cell);
            }
            int n = 1;
            for (PrescriptionItem item : consultation.getMedicines()) {
                rx.addCell(cell(String.valueOf(n++), normal));
                rx.addCell(cell(item.getMedicineName(), bold));
                rx.addCell(cell(item.getDosage(), normal));
                rx.addCell(cell(item.getFrequency(), normal));
                rx.addCell(cell(item.getDuration(), normal));
                rx.addCell(cell(item.getInstructions(), normal));
            }
            document.add(rx);
        }

        if (consultation.getNotes() != null) {
            section(document, "Advice / Notes", consultation.getNotes(), heading, normal);
        }
        if (consultation.getFollowUpDate() != null) {
            section(document, "Follow-up", consultation.getFollowUpDate().format(DATE), heading, normal);
        }

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        Paragraph signature = new Paragraph("Dr. " + (doctor != null ? doctor.getName() : ""), bold);
        signature.setAlignment(Element.ALIGN_RIGHT);
        document.add(signature);
        Paragraph footer = new Paragraph("This prescription was generated electronically by " + hospitalName
                + " and is valid without a physical signature.", small);
        footer.setSpacingBefore(20);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private static void section(Document document, String label, String text, Font heading, Font normal) {
        Paragraph h = new Paragraph(label, heading);
        h.setSpacingBefore(10);
        document.add(h);
        document.add(new Paragraph(text, normal));
    }

    private static PdfPCell block(String label, String value, Font bold, Font normal) {
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(label + "\n", bold));
        phrase.add(new Chunk(value, normal));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        return cell;
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "-" : text, font));
        cell.setPadding(5);
        return cell;
    }

    private static String pretty(String enumName) {
        String lower = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
