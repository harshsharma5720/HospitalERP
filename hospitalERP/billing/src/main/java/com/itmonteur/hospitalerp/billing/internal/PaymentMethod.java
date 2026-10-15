package com.itmonteur.hospitalerp.billing.internal;

/** How a bill was paid or refunded: at the desk (CASH, CARD, UPI), or ONLINE through Razorpay (docs/ONLINE_PAYMENT_PLAN.md). */
public enum PaymentMethod {
    CASH, CARD, UPI, ONLINE
}
