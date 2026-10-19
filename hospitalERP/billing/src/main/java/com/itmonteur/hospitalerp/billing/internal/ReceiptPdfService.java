package com.itmonteur.hospitalerp.billing.internal;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A bill's receipt as an A4 PDF (OpenPDF), with the hospital's letterhead (HOSPITAL_*, as on prescriptions):
 * the lines, subtotal, discount and total, how and when it was paid and by whom - and the refund, if any.
 * Amounts as "Rs. 1,250.00": the PDF's standard fonts have no rupee sign.
 */
@Service
public class ReceiptPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final Color BRAND = new Color(30, 99, 219);
    private static final Color LIGHT = new Color(235, 242, 255);

    private final String hospitalName;
    private final String hospitalAddress;
    private final String hospitalPhone;

    public ReceiptPdfService(@Value("${app.hospital.name:Hospital ERP}") String hospitalName,
                             @Value("${app.hospital.address:}") String hospitalAddress,
                             @Value("${app.hospital.phone:}") String hospitalPhone) {
        this.hospitalName = hospitalName;
        this.hospitalAddress = hospitalAddress;
        this.hospitalPhone = hospitalPhone;
    }

    static String money(BigDecimal amount) {
        return String.format(Locale.ENGLISH, "Rs. %,.2f", amount);
    }

    public byte[] render(Bill bill) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, out);
        document.open();

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND);
        Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        // Letterhead
        document.add(new Paragraph(hospitalName, title));
        List<String> contact = new ArrayList<>();
        if (!hospitalAddress.isBlank()) contact.add(hospitalAddress);
        if (!hospitalPhone.isBlank()) contact.add("Phone: " + hospitalPhone);
        if (!contact.isEmpty()) {
            document.add(new Paragraph(String.join("  |  ", contact), small));
        }
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Receipt " + bill.getBillNumber(), heading));
        document.add(new Paragraph(" "));

        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.addCell(block("Patient", bill.getPatientName(), bold, normal));
        info.addCell(block("Doctor", bill.getDoctorName() == null ? "-" : "Dr. " + bill.getDoctorName(), bold, normal));
        info.addCell(block("Visit date", bill.getVisitDate().format(DATE), bold, normal));
        info.addCell(block("Bill number", bill.getBillNumber(), bold, normal));
        document.add(info);
        document.add(new Paragraph(" "));

        // Lines and amounts
        PdfPTable lines = new PdfPTable(new float[]{4, 1.4f});
        lines.setWidthPercentage(100);
        lines.addCell(headerCell("Description", bold));
        lines.addCell(amountCell("Amount", bold, LIGHT));
        for (BillItem item : bill.getItems()) {
            lines.addCell(textCell(item.getDescription(), normal));
            lines.addCell(amountCell(money(item.getAmount()), normal, null));
        }
        lines.addCell(textCell("Subtotal", bold));
        lines.addCell(amountCell(money(bill.subtotal()), normal, null));
        if (bill.getDiscount().signum() > 0) {
            lines.addCell(textCell("Discount", bold));
            lines.addCell(amountCell("- " + money(bill.getDiscount()), normal, null));
        }
        lines.addCell(textCell("Total", bold));
        lines.addCell(amountCell(money(bill.total()), bold, LIGHT));
        document.add(lines);
        document.add(new Paragraph(" "));

        // Payment and refund
        if (bill.getPaidAt() != null) {
            document.add(new Paragraph("Paid " + money(bill.total()) + " by " + method(bill.getPaymentMethod())
                    + reference(bill.getPaymentReference()) + " on " + when(bill.getPaidAt())
                    + (bill.getPaidBy() == null ? "" : ", received by " + bill.getPaidBy()), normal));
        }
        if (bill.getStatus() == BillStatus.REFUND_DUE) {
            document.add(new Paragraph("Refund due: " + money(bill.total()) + " (the visit was cancelled)", bold));
        }
        if (bill.getRefundedAt() != null) {
            document.add(new Paragraph("Refunded " + money(bill.total()) + " by " + method(bill.getRefundMethod())
                    + reference(bill.getRefundReference()) + " on " + when(bill.getRefundedAt())
                    + (bill.getRefundedBy() == null ? "" : ", by " + bill.getRefundedBy()), bold));
        }

        document.close();
        return out.toByteArray();
    }

    private static String method(PaymentMethod method) {
        return method == null ? "-" : switch (method) {
            case CASH -> "cash";
            case CARD -> "card";
            case UPI -> "UPI";
            case ONLINE -> "online payment (Razorpay)";
        };
    }

    private static String reference(String reference) {
        return reference == null ? "" : " (ref. " + reference + ")";
    }

    private static String when(LocalDateTime time) {
        return time.format(DATE_TIME);
    }

    private static PdfPCell block(String label, String value, Font bold, Font normal) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(6);
        cell.addElement(new Phrase(label, bold));
        cell.addElement(new Phrase(value == null ? "-" : value, normal));
        return cell;
    }

    static PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(LIGHT);
        cell.setPadding(5);
        return cell;
    }

    static PdfPCell textCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        return cell;
    }

    static PdfPCell amountCell(String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(5);
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        return cell;
    }
}
