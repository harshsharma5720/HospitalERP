package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A stand-in for Razorpay's API (docs/ONLINE_PAYMENT_PLAN.md) on a local port: orders, payments (fetch, capture) and
 * refunds, in memory. {@link #pay} plays the patient paying on Checkout and gives what Checkout would hand the app.
 * Signatures are made here independently of the application's code.
 */
final class FakeRazorpay {

    static final String KEY_ID = "rzp_test_fake";
    static final String KEY_SECRET = "fake-key-secret";
    static final String WEBHOOK_SECRET = "fake-webhook-secret";

    /** What Checkout gives the app after a payment. */
    record Checkout(String orderId, String paymentId, String signature) {
    }

    record Order(String id, long amount, String receipt) {
    }

    private static final Pattern PAYMENT = Pattern.compile("/v1/payments/(pay_\\w+)(/capture|/refund)?");

    private final ObjectMapper json = new ObjectMapper();
    private final HttpServer server;
    private final AtomicInteger ids = new AtomicInteger();
    final Map<String, Order> orders = Collections.synchronizedMap(new LinkedHashMap<>());
    // payment id -> {order_id, amount, status}
    private final Map<String, Map<String, Object>> payments = Collections.synchronizedMap(new LinkedHashMap<>());
    /** Every call, as "METHOD /path"; the refunds asked for, as "payment id amount". */
    final List<String> calls = Collections.synchronizedList(new ArrayList<>());
    final List<String> refunds = Collections.synchronizedList(new ArrayList<>());
    volatile boolean down;
    volatile boolean refundsFail;

    FakeRazorpay() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/v1/", this::handle);
        server.start();
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    /** The patient pays an order on Checkout; {@code status}: captured, authorized or failed. */
    Checkout pay(String orderId, String status) {
        Order order = orders.get(orderId);
        String paymentId = "pay_" + ids.incrementAndGet();
        payments.put(paymentId, new LinkedHashMap<>(Map.of("order_id", orderId, "amount", order.amount(), "status", status)));
        return new Checkout(orderId, paymentId, hmac(orderId + "|" + paymentId, KEY_SECRET));
    }

    String paymentStatus(String paymentId) {
        return (String) payments.get(paymentId).get("status");
    }

    static String hmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    void reset() {
        down = false;
        refundsFail = false;
        calls.clear();
        refunds.clear();
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        calls.add(method + " " + path);
        String expectedAuth = "Basic " + java.util.Base64.getEncoder()
                .encodeToString((KEY_ID + ":" + KEY_SECRET).getBytes(StandardCharsets.UTF_8));
        if (down) {
            send(exchange, 500, Map.of("error", Map.of("description", "down")));
            return;
        }
        if (!expectedAuth.equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            send(exchange, 401, Map.of("error", Map.of("description", "Authentication failed")));
            return;
        }
        JsonNode body = method.equals("POST") ? json.readTree(exchange.getRequestBody()) : null;
        if (method.equals("POST") && path.equals("/v1/orders")) {
            String id = "order_" + ids.incrementAndGet();
            Order order = new Order(id, body.get("amount").asLong(), body.get("receipt").asText());
            orders.put(id, order);
            send(exchange, 200, Map.of("id", id, "entity", "order", "amount", order.amount(), "amount_paid", 0,
                    "currency", body.get("currency").asText(), "receipt", order.receipt(), "status", "created",
                    "attempts", 0, "notes", body.get("notes"), "created_at", 1760000000));
            return;
        }
        Matcher m = PAYMENT.matcher(path);
        if (!m.matches() || !payments.containsKey(m.group(1))) {
            send(exchange, 400, Map.of("error", Map.of("description", "The id provided does not exist")));
            return;
        }
        String paymentId = m.group(1);
        Map<String, Object> payment = payments.get(paymentId);
        if (m.group(2) == null && method.equals("GET")) {
            send(exchange, 200, paymentJson(paymentId, payment));
        } else if ("/capture".equals(m.group(2))) {
            if (!"authorized".equals(payment.get("status")) || body.get("amount").asLong() != (long) payment.get("amount")) {
                send(exchange, 400, Map.of("error", Map.of("description", "This payment has already been captured")));
                return;
            }
            payment.put("status", "captured");
            send(exchange, 200, paymentJson(paymentId, payment));
        } else if ("/refund".equals(m.group(2))) {
            if (refundsFail || !"captured".equals(payment.get("status"))) {
                send(exchange, 400, Map.of("error", Map.of("description", "The refund could not be made")));
                return;
            }
            payment.put("status", "refunded");
            String refundId = "rfnd_" + ids.incrementAndGet();
            refunds.add(paymentId + " " + body.get("amount").asLong());
            send(exchange, 200, Map.of("id", refundId, "entity", "refund", "amount", body.get("amount").asLong(),
                    "payment_id", paymentId, "speed_requested", body.get("speed").asText(), "status", "processed"));
        } else {
            send(exchange, 405, Map.of("error", Map.of("description", "Not allowed")));
        }
    }

    private static Map<String, Object> paymentJson(String paymentId, Map<String, Object> payment) {
        return Map.of("id", paymentId, "entity", "payment", "amount", payment.get("amount"), "currency", "INR",
                "status", payment.get("status"), "order_id", payment.get("order_id"), "method", "upi",
                "captured", "captured".equals(payment.get("status")));
    }

    private void send(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = json.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
