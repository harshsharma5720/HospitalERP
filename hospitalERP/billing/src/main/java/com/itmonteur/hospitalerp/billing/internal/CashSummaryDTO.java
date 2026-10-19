package com.itmonteur.hospitalerp.billing.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * What came in and went out on a day (docs/DAILY_CASH_SUMMARY_PLAN.md): per method (cash, card, UPI, online, always
 * all four), the totals, per person who took the money ({@code by} null: paid online by the patient), every payment
 * and refund in the order they happened, and the day's visits whose bills are still unpaid.
 */
public record CashSummaryDTO(LocalDate date, List<MethodTotals> methods, Totals totals, List<PersonTotals> people,
                             List<Line> lines, Unpaid unpaid) {

    public record MethodTotals(PaymentMethod method, int payments, BigDecimal taken, int refunds, BigDecimal refunded,
                               BigDecimal net) {
    }

    public record Totals(int payments, BigDecimal taken, int refunds, BigDecimal refunded, BigDecimal net) {
    }

    public record PersonTotals(String by, int payments, BigDecimal taken, int refunds, BigDecimal refunded) {
    }

    /** {@code kind}: PAYMENT or REFUND. */
    public record Line(LocalDateTime at, String kind, long billId, String billNumber, String patientName,
                       String doctorName, PaymentMethod method, String reference, String by, BigDecimal amount) {
    }

    public record Unpaid(int bills, BigDecimal amount) {
    }
}
