package com.itmonteur.hospitalerp.billing.internal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A day closed at the front desk (docs/DAILY_CASH_SUMMARY_PLAN.md); {@code difference} = counted - expected. */
public record CashCloseDTO(Long id, LocalDateTime periodStart, LocalDateTime closedAt, String closedBy,
                           BigDecimal cashIn, BigDecimal cashOut, BigDecimal expected, BigDecimal counted,
                           BigDecimal difference, String note) {

    static CashCloseDTO of(CashClose close) {
        return new CashCloseDTO(close.getId(), close.getPeriodStart(), close.getClosedAt(), close.getClosedBy(),
                close.getCashIn(), close.getCashOut(), close.getCashExpected(), close.getCashCounted(),
                close.getCashDifference(), close.getNote());
    }
}
