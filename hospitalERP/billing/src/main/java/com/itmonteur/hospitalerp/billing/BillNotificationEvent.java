package com.itmonteur.hospitalerp.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Something happened to a visit's bill that the patient (and for a refund the front desk) should hear about
 * (docs/NOTIFICATION_CENTRE_PLAN.md). Published by billing inside the change's transaction; the inbox module writes the
 * messages. Belongs to the billing module.
 *
 * @param patientUserId the login of the bill's patient account (also for a family member's visit); null for a walk-in
 *                      without one
 * @param method        how it was paid or refunded (CASH, CARD, UPI, ONLINE); null for a bill just made
 */
public record BillNotificationEvent(Kind kind, long billId, String billNumber, Long patientUserId, String patientName,
                                    String doctorName, LocalDate visitDate, BigDecimal amount, String method) {

    public enum Kind {
        BILL_READY,
        PAID,
        REFUND_DUE,
        REFUNDED
    }
}
