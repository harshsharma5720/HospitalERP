package com.itmonteur.hospitalerp.billing.internal;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The day summary PDF (docs/DAILY_CASH_SUMMARY_PLAN.md), read back as text. */
class CashSummaryPdfServiceTest {

    private static final LocalDate DAY = LocalDate.of(2021, 3, 15);
    private final CashSummaryPdfService pdf = new CashSummaryPdfService("Shreya Hospital", "12 MG Road, Pune",
            "+91 20 5555 1234", Clock.fixed(LocalDateTime.of(2026, 10, 19, 18, 45).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    private static BigDecimal rs(String amount) {
        return new BigDecimal(amount).setScale(2);
    }

    private static CashSummaryDTO.MethodTotals method(PaymentMethod method, int payments, String taken, int refunds,
                                                      String refunded, String net) {
        return new CashSummaryDTO.MethodTotals(method, payments, rs(taken), refunds, rs(refunded), rs(net));
    }

    private static CashSummaryDTO.Line line(int hour, String kind, String bill, PaymentMethod method, String reference,
                                            String by, String amount) {
        return new CashSummaryDTO.Line(DAY.atTime(hour, 0), kind, 1, bill, "Asha Rao", "Anita Rao", method, reference, by,
                rs(amount));
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
    void aDayWithPaymentsARefundAClosedDrawerAndUnpaidBills() throws IOException {
        CashSummaryDTO day = new CashSummaryDTO(DAY,
                List.of(method(PaymentMethod.CASH, 2, "1100", 1, "500", "600"), method(PaymentMethod.CARD, 1, "500", 0, "0", "500"),
                        method(PaymentMethod.UPI, 0, "0", 0, "0", "0"), method(PaymentMethod.ONLINE, 1, "500", 0, "0", "500")),
                new CashSummaryDTO.Totals(4, rs("2100"), 1, rs("500"), rs("1600")),
                List.of(new CashSummaryDTO.PersonTotals("deskmeena", 3, rs("1600"), 1, rs("500")),
                        new CashSummaryDTO.PersonTotals(null, 1, rs("500"), 0, rs("0"))),
                List.of(line(9, "PAYMENT", "B-2021-000001", PaymentMethod.CASH, null, "deskmeena", "600"),
                        line(11, "PAYMENT", "B-2021-000002", PaymentMethod.ONLINE, "pay_X1", null, "500"),
                        line(15, "REFUND", "B-2021-000003", PaymentMethod.CASH, null, "deskmeena", "500")),
                new CashSummaryDTO.Unpaid(2, rs("1000")),
                List.of(new CashCloseDTO(7L, DAY.atStartOfDay(), DAY.atTime(18, 30), "deskmeena", rs("1100"), rs("500"),
                        rs("600"), rs("550"), rs("-50"), "Change given twice")),
                null);

        String summary = text(pdf.render(day));

        assertThat(summary).contains("Shreya Hospital", "12 MG Road, Pune", "Day summary: Monday, 15 Mar 2021",
                "Printed 19 Oct 2026, 18:45", "By method", "Cash", "Rs. 1,100.00", "Card", "UPI", "Online", "Total",
                "Rs. 2,100.00", "Rs. 1,600.00", "Who took it", "deskmeena", "Online (the patient)", "Cash closes", "18:30",
                "Rs. 550.00", "- Rs. 50.00", "Change given twice",
                "Still unpaid: 2 bills of the day's visits, Rs. 1,000.00.", "Payments and refunds", "B-2021-000001",
                "Asha Rao", "Online (pay_X1)", "Refund", "- Rs. 500.00");
        assertThat(summary).doesNotContain("wasn't closed", "not closed yet");
    }

    @Test
    void anEmptyDayStillOpenAndADrawerThatGaveBackMoreThanItTook() throws IOException {
        List<CashSummaryDTO.MethodTotals> nothing = List.of(PaymentMethod.values()).stream()
                .map(method -> method(method, 0, "0", 0, "0", "0")).toList();
        CashSummaryDTO day = new CashSummaryDTO(DAY, nothing, new CashSummaryDTO.Totals(0, rs("0"), 0, rs("0"), rs("0")),
                List.of(), List.of(), new CashSummaryDTO.Unpaid(0, rs("0")), List.of(),
                new CashSummaryDTO.CashSince(DAY.minusDays(1).atTime(19, 5), 1, rs("300"), 1, rs("500"), rs("-200")));

        String summary = text(pdf.render(day));

        assertThat(summary).contains("No payments or refunds.", "The day wasn't closed.",
                "Cash since 14 Mar 2021, 19:05, not closed yet: Rs. 300.00 in, Rs. 500.00 out, - Rs. 200.00 expected in the drawer.",
                "Every bill of the day's visits is paid.", "None.");
    }

    @Test
    void differencesAndNegativeAmountsAreSigned() {
        assertThat(CashSummaryPdfService.difference(rs("20"))).isEqualTo("+ Rs. 20.00");
        assertThat(CashSummaryPdfService.difference(rs("-50"))).isEqualTo("- Rs. 50.00");
        assertThat(CashSummaryPdfService.difference(rs("0"))).isEqualTo("Rs. 0.00");
        assertThat(CashSummaryPdfService.minus(rs("-1250.5"))).isEqualTo("- Rs. 1,250.50");
        assertThat(CashSummaryPdfService.minus(rs("1250.5"))).isEqualTo("Rs. 1,250.50");
    }
}
