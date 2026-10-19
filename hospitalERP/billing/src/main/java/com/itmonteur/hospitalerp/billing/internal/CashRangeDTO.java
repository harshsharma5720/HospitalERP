package com.itmonteur.hospitalerp.billing.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Several days for admins (docs/DAILY_CASH_SUMMARY_PLAN.md): a row per day, oldest first and every day present, with
 * the cash's own net (what the drawer should have gained) and the day's closes - how many, and their differences
 * added up (null without a close); the methods, the totals and the closes of the whole range.
 */
public record CashRangeDTO(LocalDate from, LocalDate to, List<Day> days, List<CashSummaryDTO.MethodTotals> methods,
                           CashSummaryDTO.Totals totals, List<CashCloseDTO> closes) {

    public record Day(LocalDate date, int payments, BigDecimal taken, int refunds, BigDecimal refunded, BigDecimal net,
                      BigDecimal cashNet, int closes, BigDecimal difference) {
    }
}
