package com.itmonteur.hospitalerp.inbox;

/** What a message in the notification centre is about (docs/NOTIFICATION_CENTRE_PLAN.md). Stored as text. */
public enum InboxKind {
    // Visits
    VISIT_BOOKED,
    VISIT_MOVED,
    VISIT_CANCELLED,
    VISIT_CANCELLED_BY_LEAVE,
    VISIT_REMINDER,
    // Bills and payments
    BILL_READY,
    PAYMENT_RECEIVED,
    REFUND_DUE,
    REFUND_GIVEN,
    // Leave
    LEAVE_REQUESTED,
    LEAVE_APPROVED,
    LEAVE_REJECTED
}
