package com.itmonteur.hospitalerp.billing.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A bill as the patient sees it (docs/PATIENT_BILLS_PLAN.md): like the front desk's {@link BillDTO}, without who took
 * or gave back the money (staff user names) and the ids the desk needs to change it.
 */
public record MyBillDTO(Long id, String billNumber, Long appointmentId, String patientName, String doctorName,
                        LocalDate visitDate, BillStatus status, List<Item> items, BigDecimal subtotal,
                        BigDecimal discount, BigDecimal total,
                        LocalDateTime paidAt, PaymentMethod paymentMethod, String paymentReference,
                        LocalDateTime refundedAt, PaymentMethod refundMethod, String refundReference) {

    public record Item(String description, BigDecimal amount) {
    }

    static MyBillDTO of(Bill bill) {
        return new MyBillDTO(bill.getId(), bill.getBillNumber(), bill.getAppointmentId(), bill.getPatientName(),
                bill.getDoctorName(), bill.getVisitDate(), bill.getStatus(),
                bill.getItems().stream().map(i -> new Item(i.getDescription(), i.getAmount())).toList(),
                bill.subtotal(), bill.getDiscount(), bill.total(),
                bill.getPaidAt(), bill.getPaymentMethod(), bill.getPaymentReference(),
                bill.getRefundedAt(), bill.getRefundMethod(), bill.getRefundReference());
    }
}
