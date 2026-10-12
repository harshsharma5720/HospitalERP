package com.itmonteur.hospitalerp.billing.internal;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** The receipt PDF (docs/BILLING_PLAN.md, step B.3), read back as text. */
class ReceiptPdfServiceTest {

    private final ReceiptPdfService pdf = new ReceiptPdfService("Shreya Hospital", "12 MG Road, Pune", "+91 20 5555 1234");

    private static Bill paidBill() {
        Bill bill = new Bill();
        bill.setBillNumber("B-2026-000042");
        bill.setPatientName("Asha Rao");
        bill.setDoctorName("Anita Rao");
        bill.setVisitDate(LocalDate.of(2026, 10, 12));
        bill.addItem("Consultation with Dr. Anita Rao", new BigDecimal("500.00"), true);
        bill.addItem("Dressing", new BigDecimal("1250.50"), false);
        bill.setDiscount(new BigDecimal("50.50"));
        bill.setStatus(BillStatus.PAID);
        bill.setPaymentMethod(PaymentMethod.UPI);
        bill.setPaymentReference("UPI-123");
        bill.setPaidAt(LocalDateTime.of(2026, 10, 12, 10, 5));
        bill.setPaidBy("deskmeena");
        return bill;
    }

    private static String text(byte[] document) throws IOException {
        assertThat(new String(document, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(document);
        StringBuilder text = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        return text.toString();
    }

    @Test
    void aReceiptShowsTheLinesTheAmountsAndThePayment() throws IOException {
        String receipt = text(pdf.render(paidBill()));

        assertThat(receipt).contains("Shreya Hospital", "12 MG Road, Pune", "+91 20 5555 1234", "Receipt B-2026-000042",
                "Asha Rao", "Dr. Anita Rao", "12 Oct 2026", "Consultation with Dr. Anita Rao", "Rs. 500.00", "Dressing",
                "Rs. 1,250.50", "Subtotal", "Rs. 1,750.50", "Discount", "- Rs. 50.50", "Total", "Rs. 1,700.00",
                "by UPI (ref. UPI-123)", "12 Oct 2026, 10:05", "received by deskmeena");
        assertThat(receipt).doesNotContain("Refund");
    }

    @Test
    void aCancelledPaidVisitShowsTheRefundDueAndThenTheRefund() throws IOException {
        Bill bill = paidBill();
        bill.setDiscount(BigDecimal.ZERO.setScale(2));
        bill.setStatus(BillStatus.REFUND_DUE);
        assertThat(text(pdf.render(bill))).contains("Refund due: Rs. 1,750.50").doesNotContain("Discount");

        bill.setStatus(BillStatus.REFUNDED);
        bill.setRefundMethod(PaymentMethod.CASH);
        bill.setRefundedAt(LocalDateTime.of(2026, 10, 13, 9, 30));
        bill.setRefundedBy("deskravi");
        String receipt = text(pdf.render(bill));
        assertThat(receipt).contains("Refunded Rs. 1,750.50 by cash", "13 Oct 2026, 09:30", "by deskravi")
                .doesNotContain("Refund due");
    }
}
