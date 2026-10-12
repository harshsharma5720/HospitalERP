package com.itmonteur.hospitalerp.billing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** A line on a bill. The consultation fee line ({@code consultation}) is made with the bill and can't be removed. */
@Entity
@Table(name = "bill_items")
public class BillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean consultation;

    protected BillItem() {
    }

    BillItem(Bill bill, String description, BigDecimal amount, boolean consultation) {
        this.bill = bill;
        this.description = description;
        this.amount = amount;
        this.consultation = consultation;
    }

    public Long getId() { return id; }

    public Bill getBill() { return bill; }

    public String getDescription() { return description; }

    public BigDecimal getAmount() { return amount; }

    public boolean isConsultation() { return consultation; }
}
