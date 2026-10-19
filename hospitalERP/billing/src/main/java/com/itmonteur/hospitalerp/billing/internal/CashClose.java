package com.itmonteur.hospitalerp.billing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A day closed at the front desk (docs/DAILY_CASH_SUMMARY_PLAN.md): the cash taken and given back from
 * {@code periodStart} to {@code closedAt}, what the drawer should have held, what was counted, and the difference
 * (counted - expected: negative when cash is missing). Never changed once made.
 */
@Entity
@Table(name = "cash_closes")
public class CashClose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique: two closes made at the same moment would cover the same cash
    @Column(nullable = false, unique = true)
    private LocalDateTime periodStart;

    @Column(nullable = false)
    private LocalDateTime closedAt;

    @Column(nullable = false, length = 100)
    private String closedBy;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cashIn;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cashOut;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cashExpected;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cashCounted;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cashDifference;

    @Column(length = 500)
    private String note;

    protected CashClose() {
    }

    CashClose(CashSummaryDTO.CashSince since, LocalDateTime closedAt, String closedBy, BigDecimal counted, String note) {
        this.periodStart = since.from();
        this.closedAt = closedAt;
        this.closedBy = closedBy;
        this.cashIn = since.cashIn();
        this.cashOut = since.cashOut();
        this.cashExpected = since.expected();
        this.cashCounted = counted;
        this.cashDifference = counted.subtract(since.expected());
        this.note = note;
    }

    public Long getId() { return id; }
    public LocalDateTime getPeriodStart() { return periodStart; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getClosedBy() { return closedBy; }
    public BigDecimal getCashIn() { return cashIn; }
    public BigDecimal getCashOut() { return cashOut; }
    public BigDecimal getCashExpected() { return cashExpected; }
    public BigDecimal getCashCounted() { return cashCounted; }
    public BigDecimal getCashDifference() { return cashDifference; }
    public String getNote() { return note; }
}
