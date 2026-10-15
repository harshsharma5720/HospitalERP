package com.itmonteur.hospitalerp.billing.internal;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Razorpay's signatures (docs/ONLINE_PAYMENT_PLAN.md): HMAC-SHA256 as lowercase hex. Checkout signs
 * "order id|payment id" with the key secret; a webhook signs its raw body with the webhook secret.
 */
final class RazorpaySignatures {

    private RazorpaySignatures() {
    }

    static String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    /** True when {@code signature} is the data's signature; compared in constant time. */
    static boolean matches(String data, String signature, String secret) {
        if (data == null || signature == null || secret == null || secret.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(sign(data, secret).getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
