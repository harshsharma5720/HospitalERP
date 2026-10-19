package com.itmonteur.hospitalerp.billing.internal;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.itmonteur.hospitalerp.billing.internal.ReceiptPdfService.amountCell;
import static com.itmonteur.hospitalerp.billing.internal.ReceiptPdfService.headerCell;
import static com.itmonteur.hospitalerp.billing.internal.ReceiptPdfService.money;
import static com.itmonteur.hospitalerp.billing.internal.ReceiptPdfService.textCell;

/**
 * A day's cash summary as an A4 PDF (docs/DAILY_CASH_SUMMARY_PLAN.md), in the receipts' style and letterhead: the
 * totals per method, who took what, the closes, the visits still unpaid, and every payment and refund.
 */
@Service
public class CashSummaryPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final Color BRAND = new Color(30, 99, 219);
    private static final Color LIGHT = new Color(235, 242, 255);

    private final String hospitalName;
    private final String hospitalAddress;
    private final String hospitalPhone;
    private final Clock clock;

    public CashSummaryPdfService(@Value("${app.hospital.name:Hospital ERP}") String hospitalName,
                                 @Value("${app.hospital.address:}") String hospitalAddress,
                                 @Value("${app.hospital.phone:}") String hospitalPhone, Clock clock) {
        this.hospitalName = hospitalName;
        this.hospitalAddress = hospitalAddress;
        this.hospitalPhone = hospitalPhone;
        this.clock = clock;
    }

    public byte[] render(CashSummaryDTO day) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, out);
        document.open();

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND);
        Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font section = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
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
        document.add(new Paragraph("Day summary: " + day.date().format(DATE), heading));
        document.add(new Paragraph("Printed " + LocalDateTime.now(clock).format(DATE_TIME), small));
        document.add(new Paragraph(" "));

        // Per method, and the day's total
        document.add(new Paragraph("By method", section));
        PdfPTable methods = table(new float[]{2, 1, 1.5f, 1, 1.5f, 1.5f});
        for (String label : List.of("Method", "Payments", "Taken", "Refunds", "Refunded", "Net")) {
            methods.addCell(label.equals("Method") ? headerCell(label, bold) : amountCell(label, bold, LIGHT));
        }
        for (CashSummaryDTO.MethodTotals method : day.methods()) {
            methods.addCell(textCell(label(method.method()), normal));
            amounts(methods, method.payments(), method.taken(), method.refunds(), method.refunded(), method.net(), normal, null);
        }
        CashSummaryDTO.Totals totals = day.totals();
        methods.addCell(headerCell("Total", bold));
        amounts(methods, totals.payments(), totals.taken(), totals.refunds(), totals.refunded(), totals.net(), bold, LIGHT);
        document.add(methods);

        // Who took the money
        document.add(new Paragraph("Who took it", section));
        if (day.people().isEmpty()) {
            document.add(new Paragraph("No payments or refunds.", normal));
        } else {
            PdfPTable people = table(new float[]{3, 1, 1.5f, 1, 1.5f});
            for (String label : List.of("Who", "Payments", "Taken", "Refunds", "Refunded")) {
                people.addCell(label.equals("Who") ? headerCell(label, bold) : amountCell(label, bold, LIGHT));
            }
            for (CashSummaryDTO.PersonTotals person : day.people()) {
                people.addCell(textCell(person.by() == null ? "Online (the patient)" : person.by(), normal));
                people.addCell(amountCell(String.valueOf(person.payments()), normal, null));
                people.addCell(amountCell(money(person.taken()), normal, null));
                people.addCell(amountCell(String.valueOf(person.refunds()), normal, null));
                people.addCell(amountCell(money(person.refunded()), normal, null));
            }
            document.add(people);
        }

        // The closes
        document.add(new Paragraph("Cash closes", section));
        if (day.closes().isEmpty()) {
            document.add(new Paragraph("The day wasn't closed.", normal));
        } else {
            PdfPTable closes = table(new float[]{1, 1.6f, 1.4f, 1.4f, 1.4f, 3});
            for (String label : List.of("Closed", "By", "Expected", "Counted", "Difference", "Note")) {
                closes.addCell(List.of("Expected", "Counted", "Difference").contains(label) ? amountCell(label, bold, LIGHT)
                        : headerCell(label, bold));
            }
            for (CashCloseDTO close : day.closes()) {
                closes.addCell(textCell(close.closedAt().format(TIME), normal));
                closes.addCell(textCell(close.closedBy(), normal));
                closes.addCell(amountCell(money(close.expected()), normal, null));
                closes.addCell(amountCell(money(close.counted()), normal, null));
                closes.addCell(amountCell(difference(close.difference()), close.difference().signum() == 0 ? normal : bold, null));
                closes.addCell(textCell(close.note() == null ? "" : close.note(), normal));
            }
            document.add(closes);
        }
        if (day.sinceLastClose() != null) {
            CashSummaryDTO.CashSince since = day.sinceLastClose();
            document.add(new Paragraph("Cash since " + since.from().format(DATE_TIME) + ", not closed yet: "
                    + money(since.cashIn()) + " in, " + money(since.cashOut()) + " out, " + minus(since.expected())
                    + " expected in the drawer.", normal));
        }

        // Still unpaid
        document.add(new Paragraph(" "));
        document.add(new Paragraph(day.unpaid().bills() == 0 ? "Every bill of the day's visits is paid."
                : "Still unpaid: " + day.unpaid().bills() + (day.unpaid().bills() == 1 ? " bill" : " bills")
                + " of the day's visits, " + money(day.unpaid().amount()) + ".", normal));

        // Every payment and refund
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Payments and refunds", section));
        if (day.lines().isEmpty()) {
            document.add(new Paragraph("None.", normal));
        } else {
            PdfPTable lines = table(new float[]{0.8f, 1, 1.6f, 2.4f, 1.6f, 1.6f, 1.4f});
            for (String label : List.of("Time", "", "Bill", "Patient", "Method", "By", "Amount")) {
                lines.addCell(label.equals("Amount") ? amountCell(label, bold, LIGHT) : headerCell(label, bold));
            }
            lines.setHeaderRows(1); // repeated on every page
            for (CashSummaryDTO.Line line : day.lines()) {
                boolean payment = "PAYMENT".equals(line.kind());
                lines.addCell(textCell(line.at().format(TIME), normal));
                lines.addCell(textCell(payment ? "Payment" : "Refund", normal));
                lines.addCell(textCell(line.billNumber(), normal));
                lines.addCell(textCell(line.patientName(), normal));
                lines.addCell(textCell(label(line.method()) + (line.reference() == null ? "" : " (" + line.reference() + ")"), normal));
                lines.addCell(textCell(line.by() == null ? "Online" : line.by(), normal));
                lines.addCell(amountCell((payment ? "" : "- ") + money(line.amount()), normal, null));
            }
            document.add(lines);
        }

        document.close();
        return out.toByteArray();
    }

    private static PdfPTable table(float[] widths) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(10);
        return table;
    }

    private static void amounts(PdfPTable table, int payments, BigDecimal taken, int refunds, BigDecimal refunded,
                                BigDecimal net, Font font, Color background) {
        table.addCell(amountCell(String.valueOf(payments), font, background));
        table.addCell(amountCell(money(taken), font, background));
        table.addCell(amountCell(String.valueOf(refunds), font, background));
        table.addCell(amountCell(money(refunded), font, background));
        table.addCell(amountCell(minus(net), font, background));
    }

    // "- Rs. 500.00" below 0, as the receipts show a discount
    static String minus(BigDecimal amount) {
        return amount.signum() < 0 ? "- " + money(amount.negate()) : money(amount);
    }

    // "+ Rs. 20.00" (more than expected), "- Rs. 50.00" (missing), "Rs. 0.00"
    static String difference(BigDecimal difference) {
        return difference.signum() > 0 ? "+ " + money(difference) : minus(difference);
    }

    static String label(PaymentMethod method) {
        return method == null ? "-" : switch (method) {
            case CASH -> "Cash";
            case CARD -> "Card";
            case UPI -> "UPI";
            case ONLINE -> "Online";
        };
    }
}
