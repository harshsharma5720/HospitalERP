package com.itmonteur.hospitalerp.billing.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A bill as the front desk sees it (docs/BILLING_PLAN.md): its lines, the amounts, and the payment / refund.
 * {@code unappliedPayments}: online payments that came when the bill was already paid, changed or cancelled and were
 * given back - or must be, by hand, when the automatic refund failed (docs/ONLINE_PAYMENT_PLAN.md).
 */
public record BillDTO(Long id, String billNumber, Long appointmentId, Long patientId, String patientName,
                      String doctorName, LocalDate visitDate, BillStatus status, List<Item> items, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total, LocalDateTime createdAt,
                      LocalDateTime paidAt, PaymentMethod paymentMethod, String paymentReference, String paidBy,
                      LocalDateTime refundedAt, PaymentMethod refundMethod, String refundReference, String refundedBy,
                      List<UnappliedPayment> unappliedPayments) {

    /** A line; {@code consultation}: the doctor's fee, which can't be removed. */
    public record Item(Long id, String description, BigDecimal amount, boolean consultation) {
    }

    /** {@code status}: REFUNDING, REFUNDED or REFUND_FAILED; {@code createdAt}: when the patient started paying. */
    public record UnappliedPayment(String paymentId, BigDecimal amount, OnlinePaymentStatus status, String note,
                                   String refundId, LocalDateTime createdAt) {
    }

    static BillDTO of(Bill bill, List<OnlinePayment> unapplied) {
        return new BillDTO(bill.getId(), bill.getBillNumber(), bill.getAppointmentId(), bill.getPatientId(),
                bill.getPatientName(), bill.getDoctorName(), bill.getVisitDate(), bill.getStatus(),
                bill.getItems().stream()
                        .map(i -> new Item(i.getId(), i.getDescription(), i.getAmount(), i.isConsultation()))
                        .toList(),
                bill.subtotal(), bill.getDiscount(), bill.total(), bill.getCreatedAt(),
                bill.getPaidAt(), bill.getPaymentMethod(), bill.getPaymentReference(), bill.getPaidBy(),
                bill.getRefundedAt(), bill.getRefundMethod(), bill.getRefundReference(), bill.getRefundedBy(),
                unapplied.stream().map(p -> new UnappliedPayment(p.getPaymentId(), p.getAmount(), p.getStatus(),
                        p.getNote(), p.getRefundId(), p.getCreatedAt())).toList());
    }
}
