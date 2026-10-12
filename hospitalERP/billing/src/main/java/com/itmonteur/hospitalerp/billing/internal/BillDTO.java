package com.itmonteur.hospitalerp.billing.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** A bill as the front desk sees it (docs/BILLING_PLAN.md): its lines, the amounts, and the payment / refund. */
public record BillDTO(Long id, String billNumber, Long appointmentId, Long patientId, String patientName,
                      String doctorName, LocalDate visitDate, BillStatus status, List<Item> items, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total, LocalDateTime createdAt,
                      LocalDateTime paidAt, PaymentMethod paymentMethod, String paymentReference, String paidBy,
                      LocalDateTime refundedAt, PaymentMethod refundMethod, String refundReference, String refundedBy) {

    /** A line; {@code consultation}: the doctor's fee, which can't be removed. */
    public record Item(Long id, String description, BigDecimal amount, boolean consultation) {
    }

    static BillDTO of(Bill bill) {
        return new BillDTO(bill.getId(), bill.getBillNumber(), bill.getAppointmentId(), bill.getPatientId(),
                bill.getPatientName(), bill.getDoctorName(), bill.getVisitDate(), bill.getStatus(),
                bill.getItems().stream()
                        .map(i -> new Item(i.getId(), i.getDescription(), i.getAmount(), i.isConsultation()))
                        .toList(),
                bill.subtotal(), bill.getDiscount(), bill.total(), bill.getCreatedAt(),
                bill.getPaidAt(), bill.getPaymentMethod(), bill.getPaymentReference(), bill.getPaidBy(),
                bill.getRefundedAt(), bill.getRefundMethod(), bill.getRefundReference(), bill.getRefundedBy());
    }
}
