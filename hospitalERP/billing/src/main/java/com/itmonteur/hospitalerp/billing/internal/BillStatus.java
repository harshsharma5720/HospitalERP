package com.itmonteur.hospitalerp.billing.internal;

/**
 * UNPAID → PAID; UNPAID → CANCELLED when the patient leaves the queue or the visit is cancelled (reopened at the next
 * check-in); PAID → REFUND_DUE when the visit is cancelled → REFUNDED (docs/BILLING_PLAN.md).
 */
public enum BillStatus {
    UNPAID, PAID, CANCELLED, REFUND_DUE, REFUNDED
}
