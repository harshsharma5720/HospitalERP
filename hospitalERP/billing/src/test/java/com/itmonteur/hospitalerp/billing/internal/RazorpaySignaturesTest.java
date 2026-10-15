package com.itmonteur.hospitalerp.billing.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Razorpay's HMAC-SHA256 signatures (docs/ONLINE_PAYMENT_PLAN.md); the value was computed outside Java. */
class RazorpaySignaturesTest {

    private static final String SIGNED = "7d3e8a8e74679a8f3301eaeeb04a491bcbe0e5602892077253d7990633aa14fa";

    @Test
    void signsAsLowercaseHex() {
        assertThat(RazorpaySignatures.sign("order_ABC|pay_XYZ", "s3cret")).isEqualTo(SIGNED);
    }

    @Test
    void acceptsOnlyTheRightSignature() {
        assertThat(RazorpaySignatures.matches("order_ABC|pay_XYZ", SIGNED, "s3cret")).isTrue();
        assertThat(RazorpaySignatures.matches("order_ABC|pay_XYZ", SIGNED.toUpperCase(), "s3cret")).isTrue();
        assertThat(RazorpaySignatures.matches("order_ABC|pay_XYZ", SIGNED, "other")).isFalse();
        assertThat(RazorpaySignatures.matches("order_ABC|pay_XYY", SIGNED, "s3cret")).isFalse();
        assertThat(RazorpaySignatures.matches("order_ABC|pay_XYZ", null, "s3cret")).isFalse();
        assertThat(RazorpaySignatures.matches("order_ABC|pay_XYZ", SIGNED, "")).as("no secret set").isFalse();
    }
}
