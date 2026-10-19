package com.itmonteur.hospitalerp.billing.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Several days for admins (docs/DAILY_CASH_SUMMARY_PLAN.md): a row per day, oldest first and every day present, with
 * the cash's own net (what the drawer should have gained); the methods and totals of the whole range.
 */
public record CashRangeDTO(LocalDate from, LocalDate to, List<Day> days, List<CashSummaryDTO.MethodTotals> methods,
                           CashSummaryDTO.Totals totals) {

    public record Day(LocalDate date, int payments, BigDecimal taken, int refunds, BigDecimal refunded, BigDecimal net,
                      BigDecimal cashNet) {
    }
}
