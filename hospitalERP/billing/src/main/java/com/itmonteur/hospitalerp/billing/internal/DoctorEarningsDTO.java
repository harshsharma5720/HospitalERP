package com.itmonteur.hospitalerp.billing.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * What a doctor's visits in a period brought in (docs/DOCTOR_ANALYTICS_PLAN.md), from their bills, by visit date:
 * {@code earned} = paid and not refunded (also per day); {@code averagePerVisit} = earned / paid visits, null without
 * any; {@code refunds} = due for refund or refunded; {@code unpaid} = still open. Cancelled bills (nothing was paid)
 * are left out. Rupees with 2 decimals.
 */
public record DoctorEarningsDTO(Period period, BigDecimal earned, long paidVisits, BigDecimal averagePerVisit,
                                BigDecimal refunds, long refundedVisits, BigDecimal unpaid, long unpaidVisits,
                                List<Day> perDay) {

    public record Period(int days, LocalDate from, LocalDate to) {
    }

    public record Day(LocalDate date, BigDecimal earned) {
    }
}
