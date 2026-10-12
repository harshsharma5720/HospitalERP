package com.itmonteur.hospitalerp.billing.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A bill in the front desk's list. */
public record BillSummaryDTO(Long id, String billNumber, Long appointmentId, Long patientId, String patientName,
                             String doctorName, LocalDate visitDate, BillStatus status, BigDecimal total) {

    static BillSummaryDTO of(Bill bill) {
        return new BillSummaryDTO(bill.getId(), bill.getBillNumber(), bill.getAppointmentId(), bill.getPatientId(),
                bill.getPatientName(), bill.getDoctorName(), bill.getVisitDate(), bill.getStatus(), bill.total());
    }
}
