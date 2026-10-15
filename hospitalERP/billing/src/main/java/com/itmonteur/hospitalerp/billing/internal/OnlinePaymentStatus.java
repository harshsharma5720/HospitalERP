package com.itmonteur.hospitalerp.billing.internal;

/**
 * An online payment attempt (docs/ONLINE_PAYMENT_PLAN.md): CREATED (the Razorpay order, not paid yet) → PAID (the
 * payment paid the bill), or → REFUNDING → REFUNDED / REFUND_FAILED when the payment couldn't be applied (the bill was
 * paid, changed or cancelled meanwhile) and was given back.
 */
public enum OnlinePaymentStatus {
    CREATED, PAID, REFUNDING, REFUNDED, REFUND_FAILED
}
