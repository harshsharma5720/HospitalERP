package com.itmonteur.hospitalerp.billing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A Razorpay order made for a bill (docs/ONLINE_PAYMENT_PLAN.md), with the payment that paid it, or the refund when
 * the payment couldn't be applied. A bill can have several: a new order is made when the bill's total changed.
 */
@Entity
@Table(name = "online_payments")
public class OnlinePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @Column(nullable = false, unique = true, length = 40)
    private String orderId;

    /** The order's amount in rupees (the bill's total when it was made). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 13)
    private OnlinePaymentStatus status;

    @Column(length = 40)
    private String paymentId;

    @Column(length = 40)
    private String refundId;

    /** Why the payment was given back. */
    @Column(length = 200)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;

    protected OnlinePayment() {
    }

    OnlinePayment(Bill bill, String orderId, BigDecimal amount, LocalDateTime createdAt) {
        this.bill = bill;
        this.orderId = orderId;
        this.amount = amount;
        this.status = OnlinePaymentStatus.CREATED;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Bill getBill() { return bill; }
    public String getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public OnlinePaymentStatus getStatus() { return status; }
    public void setStatus(OnlinePaymentStatus status) { this.status = status; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
}
