package com.itmonteur.hospitalerp.billing.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The calls {@link RazorpayClient} makes (docs/ONLINE_PAYMENT_PLAN.md), against a local HTTP server that answers like
 * Razorpay (with fields the client doesn't read) and records what it was asked.
 */
class RazorpayClientTest {

    private record Request(String method, String path, String auth, JsonNode body) {
    }

    private final ObjectMapper json = new ObjectMapper();
    private final List<Request> requests = new ArrayList<>();
    private HttpServer server;
    private String answer;
    private int status;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/", exchange -> {
            byte[] in = exchange.getRequestBody().readAllBytes();
            requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"), in.length == 0 ? null : json.readTree(in)));
            byte[] out = answer.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        status = 200;
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private RazorpayClient client(String keyId, String keySecret, String webhookSecret) {
        return new RazorpayClient(RestClient.builder(), keyId, keySecret, webhookSecret,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
    }

    private RazorpayClient client() {
        return client("rzp_test_1", "secret1", "hook1");
    }

    @Test
    void createsAnOrderInPaiseWithTheKeysAsBasicAuth() {
        answer = """
                {"id":"order_9A33XWu170gUtm","entity":"order","amount":50000,"amount_paid":0,"amount_due":50000,
                 "currency":"INR","receipt":"B-2026-000012","offer_id":null,"status":"created","attempts":0,
                 "notes":{"billNumber":"B-2026-000012"},"created_at":1760000000}""";
        RazorpayClient.Order order = client().createOrder(50000, "B-2026-000012", Map.of("billNumber", "B-2026-000012"));

        assertThat(order).isEqualTo(new RazorpayClient.Order("order_9A33XWu170gUtm", 50000, "created"));
        Request sent = requests.get(0);
        assertThat(sent.method() + " " + sent.path()).isEqualTo("POST /v1/orders");
        assertThat(sent.auth()).isEqualTo("Basic " + Base64.getEncoder().encodeToString("rzp_test_1:secret1".getBytes()));
        assertThat(sent.body().get("amount").asLong()).isEqualTo(50000);
        assertThat(sent.body().get("currency").asText()).isEqualTo("INR");
        assertThat(sent.body().get("receipt").asText()).isEqualTo("B-2026-000012");
        assertThat(sent.body().at("/notes/billNumber").asText()).isEqualTo("B-2026-000012");
    }

    @Test
    void fetchesCapturesAndRefundsPayments() {
        answer = """
                {"id":"pay_29QQoUBi66xm2f","entity":"payment","amount":50000,"currency":"INR","status":"authorized",
                 "order_id":"order_9A33XWu170gUtm","method":"upi","captured":false,"email":"a@example.com"}""";
        assertThat(client().fetchPayment("pay_29QQoUBi66xm2f")).isEqualTo(new RazorpayClient.Payment(
                "pay_29QQoUBi66xm2f", "order_9A33XWu170gUtm", 50000, "authorized", false));
        assertThat(requests.get(0).method() + " " + requests.get(0).path()).isEqualTo("GET /v1/payments/pay_29QQoUBi66xm2f");

        answer = answer.replace("\"authorized\"", "\"captured\"").replace("false", "true");
        assertThat(client().capture("pay_29QQoUBi66xm2f", 50000).status()).isEqualTo("captured");
        assertThat(requests.get(1).path()).isEqualTo("/v1/payments/pay_29QQoUBi66xm2f/capture");
        assertThat(requests.get(1).body().get("amount").asLong()).isEqualTo(50000);
        assertThat(requests.get(1).body().get("currency").asText()).isEqualTo("INR");

        answer = """
                {"id":"rfnd_FP8QHiV938haTz","entity":"refund","amount":50000,"receipt":null,"currency":"INR",
                 "payment_id":"pay_29QQoUBi66xm2f","notes":[],"status":"processed","speed_requested":"normal"}""";
        assertThat(client().refund("pay_29QQoUBi66xm2f", 50000, Map.of("reason", "paid twice"))).isEqualTo(
                new RazorpayClient.Refund("rfnd_FP8QHiV938haTz", "pay_29QQoUBi66xm2f", 50000, "processed"));
        Request refund = requests.get(2);
        assertThat(refund.path()).isEqualTo("/v1/payments/pay_29QQoUBi66xm2f/refund");
        assertThat(refund.body().get("speed").asText()).isEqualTo("normal");
        assertThat(refund.body().at("/notes/reason").asText()).isEqualTo("paid twice");
    }

    @Test
    void aRefusedOrFailedCallIsARazorpayError() {
        status = 400;
        answer = "{\"error\":{\"code\":\"BAD_REQUEST_ERROR\",\"description\":\"The id provided does not exist\"}}";
        assertThatThrownBy(() -> client().fetchPayment("pay_missing"))
                .isInstanceOf(RazorpayClient.RazorpayException.class)
                .hasMessageStartingWith("Could not fetch a payment at Razorpay");
        RazorpayClient unreachable = new RazorpayClient(RestClient.builder(), "rzp_test_1", "secret1", "",
                "http://127.0.0.1:1/v1");
        assertThatThrownBy(() -> unreachable.createOrder(100, "B-1", Map.of()))
                .isInstanceOf(RazorpayClient.RazorpayException.class);
    }

    @Test
    void offWithoutBothKeys() {
        assertThat(client().enabled()).isTrue();
        assertThat(client("rzp_test_1", " ", "").enabled()).isFalse();
        assertThat(client("", "secret1", "").enabled()).isFalse();
        assertThatThrownBy(() -> client("", "", "").fetchPayment("pay_1")).isInstanceOf(IllegalStateException.class);
        assertThat(requests).isEmpty();
    }

    @Test
    void checksCheckoutAndWebhookSignatures() {
        RazorpayClient client = client();
        String body = "{\"event\":\"payment.captured\"}";
        assertThat(client.validPaymentSignature("order_1", "pay_1", RazorpaySignatures.sign("order_1|pay_1", "secret1"))).isTrue();
        assertThat(client.validPaymentSignature("order_1", "pay_2", RazorpaySignatures.sign("order_1|pay_1", "secret1"))).isFalse();
        assertThat(client.validWebhookSignature(body, RazorpaySignatures.sign(body, "hook1"))).isTrue();
        assertThat(client.validWebhookSignature(body, RazorpaySignatures.sign(body, "secret1"))).as("the key secret").isFalse();
        assertThat(client("rzp_test_1", "secret1", "").validWebhookSignature(body, RazorpaySignatures.sign(body, "any")))
                .as("no webhook secret: every webhook is refused").isFalse();
    }
}
