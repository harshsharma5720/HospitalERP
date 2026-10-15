package com.itmonteur.hospitalerp.billing.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Razorpay's REST API (docs/ONLINE_PAYMENT_PLAN.md): orders, payments (fetch, capture) and refunds, with the key id
 * and secret as basic auth. Amounts are in paise. Online payment is on only when both keys are set
 * (RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET); RAZORPAY_API_URL points the tests at a fake.
 */
@Component
public class RazorpayClient {

    /** Thrown when Razorpay can't be reached or refuses a call. */
    static class RazorpayException extends RuntimeException {
        RazorpayException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Order(String id, long amount, String status) {
    }

    /** {@code status}: created, authorized, captured, refunded or failed. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Payment(String id, @JsonProperty("order_id") String orderId, long amount, String status, boolean captured) {
    }

    /** {@code status}: pending, processed or failed. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Refund(String id, @JsonProperty("payment_id") String paymentId, long amount, String status) {
    }

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final RestClient rest;

    public RazorpayClient(RestClient.Builder builder,
                          @Value("${app.razorpay.key-id:}") String keyId,
                          @Value("${app.razorpay.key-secret:}") String keySecret,
                          @Value("${app.razorpay.webhook-secret:}") String webhookSecret,
                          @Value("${app.razorpay.api-url:https://api.razorpay.com/v1}") String apiUrl) {
        this.keyId = keyId.trim();
        this.keySecret = keySecret.trim();
        this.webhookSecret = webhookSecret.trim();
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(5));
        timeouts.setReadTimeout(Duration.ofSeconds(20));
        this.rest = builder.baseUrl(apiUrl)
                .requestFactory(timeouts)
                .defaultHeaders(headers -> headers.setBasicAuth(this.keyId, this.keySecret))
                .build();
    }

    /** Online payment is on: both keys are set. */
    public boolean enabled() {
        return !keyId.isEmpty() && !keySecret.isEmpty();
    }

    /** The public key id, for Checkout. */
    String keyId() {
        return keyId;
    }

    /** An order of {@code amount} paise in rupees; {@code receipt}: at most 40 characters. */
    Order createOrder(long amount, String receipt, Map<String, String> notes) {
        return call("create an order", () -> rest.post().uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount, "currency", "INR", "receipt", receipt, "notes", notes))
                .retrieve().body(Order.class));
    }

    Payment fetchPayment(String paymentId) {
        return call("fetch a payment", () -> rest.get().uri("/payments/{id}", paymentId).retrieve().body(Payment.class));
    }

    /** Captures an authorized payment (for when the account doesn't capture automatically). */
    Payment capture(String paymentId, long amount) {
        return call("capture a payment", () -> rest.post().uri("/payments/{id}/capture", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount, "currency", "INR"))
                .retrieve().body(Payment.class));
    }

    /** Gives back {@code amount} paise of a captured payment, at normal speed (5-7 working days). */
    Refund refund(String paymentId, long amount, Map<String, String> notes) {
        return call("refund a payment", () -> rest.post().uri("/payments/{id}/refund", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount, "speed", "normal", "notes", notes))
                .retrieve().body(Refund.class));
    }

    /** Checkout's signature of a payment: "order id|payment id" signed with the key secret. */
    boolean validPaymentSignature(String orderId, String paymentId, String signature) {
        return RazorpaySignatures.matches(orderId + "|" + paymentId, signature, keySecret);
    }

    /** A webhook's X-Razorpay-Signature: the raw body signed with the webhook secret (none set: always false). */
    boolean validWebhookSignature(String body, String signature) {
        return RazorpaySignatures.matches(body, signature, webhookSecret);
    }

    private <T> T call(String what, Supplier<T> request) {
        if (!enabled()) {
            throw new IllegalStateException("Online payment is not configured");
        }
        try {
            T answer = request.get();
            if (answer == null) {
                throw new RazorpayException("Razorpay gave no answer to " + what, null);
            }
            return answer;
        } catch (RestClientException e) {
            throw new RazorpayException("Could not " + what + " at Razorpay: " + e.getMessage(), e);
        }
    }
}
