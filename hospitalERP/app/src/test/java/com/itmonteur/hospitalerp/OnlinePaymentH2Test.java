package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Patients paying their bills online (docs/ONLINE_PAYMENT_PLAN.md) against {@link FakeRazorpay}. Its own database and
 * context (Razorpay switched on), closed after the class.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:payments;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123",
        "RAZORPAY_KEY_ID=" + FakeRazorpay.KEY_ID, "RAZORPAY_KEY_SECRET=" + FakeRazorpay.KEY_SECRET,
        "RAZORPAY_WEBHOOK_SECRET=" + FakeRazorpay.WEBHOOK_SECRET
})
@DirtiesContext
class OnlinePaymentH2Test {

    private static final FakeRazorpay razorpay = new FakeRazorpay();
    private static final String MY = "/api/bills/my";
    private static final String BILLS = "/api/receptionist/bills";
    private static final String WEBHOOK = "/api/payments/razorpay/webhook";

    @DynamicPropertySource
    static void razorpayUrl(DynamicPropertyRegistry registry) {
        registry.add("RAZORPAY_API_URL", razorpay::url);
    }

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
        razorpay.reset();
    }

    /** A patient's visit today, checked in, and its unpaid bill. */
    private record Visit(String patient, long appointmentId, long billId, String billNumber) {
    }

    private long doctor(String username, String fee) throws Exception {
        long doctorId = api.doctorWithSchedule(username).id();
        assertThat(api.call(HttpMethod.PUT, "/api/admin/fees/" + doctorId, api.admin(), "{\"consultationFee\":" + fee + "}")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        return doctorId;
    }

    // A new patient books the doctor's first free slot of the shift, then checks in at the desk
    private Visit visit(String username, String desk, long doctorId, String shift) throws Exception {
        String patient = api.registerPatient(username);
        long slotId = api.freeSlotsTomorrow(patient, doctorId, shift).get(0).get("id").asLong();
        long appointmentId = api.bookedForToday(patient, slotId, null, jdbc);
        assertThat(api.call(HttpMethod.POST, "/api/receptionist/appointments/" + appointmentId + "/check-in", desk, null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode bill = api.get(desk, BILLS + "/by-appointment/" + appointmentId);
        return new Visit(patient, appointmentId, bill.get("id").asLong(), bill.get("billNumber").asText());
    }

    private JsonNode expect(ResponseEntity<String> res, HttpStatus status) throws Exception {
        assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(status);
        return json.readTree(res.getBody());
    }

    private JsonNode start(String token, long billId, HttpStatus status) throws Exception {
        return expect(api.call(HttpMethod.POST, MY + "/" + billId + "/online-payment", token, null), status);
    }

    private String startOrder(Visit visit) throws Exception {
        return start(visit.patient(), visit.billId(), HttpStatus.OK).get("orderId").asText();
    }

    private JsonNode confirm(String token, long billId, String orderId, String paymentId, String signature,
                             HttpStatus status) throws Exception {
        return expect(api.call(HttpMethod.POST, MY + "/" + billId + "/online-payment/confirm", token,
                json.writeValueAsString(Map.of("orderId", orderId, "paymentId", paymentId, "signature", signature))), status);
    }

    private JsonNode confirm(Visit visit, FakeRazorpay.Checkout paid, HttpStatus status) throws Exception {
        return confirm(visit.patient(), visit.billId(), paid.orderId(), paid.paymentId(), paid.signature(), status);
    }

    private JsonNode atDesk(String desk, Visit visit) throws Exception {
        return api.get(desk, BILLS + "/" + visit.billId());
    }

    private Map<String, Object> attempt(String orderId) {
        return jdbc.queryForMap("SELECT * FROM online_payments WHERE order_id = ?", orderId);
    }

    // A Razorpay webhook event about a payment, signed with the secret (none: no signature header)
    private ResponseEntity<String> webhook(String event, FakeRazorpay.Checkout payment, String secret) {
        String body = "{\"entity\":\"event\",\"event\":\"" + event + "\",\"contains\":[\"payment\"],\"payload\":"
                + "{\"payment\":{\"entity\":{\"id\":\"" + payment.paymentId() + "\",\"order_id\":\"" + payment.orderId()
                + "\",\"amount\":50000,\"currency\":\"INR\",\"status\":\"captured\"}}},\"created_at\":1760000000}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-razorpay-event-id", "evt_" + payment.paymentId());
        if (secret != null) {
            headers.set("X-Razorpay-Signature", FakeRazorpay.hmac(body, secret));
        }
        return rest.exchange(WEBHOOK, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private void cancel(String desk, Visit visit) {
        assertThat(api.call(HttpMethod.DELETE, "/api/receptionist/deleteAppointment/" + visit.appointmentId(), desk, null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private long refundCalls(String paymentId) {
        return razorpay.calls.stream().filter(c -> c.equals("POST /v1/payments/" + paymentId + "/refund")).count();
    }

    @Test
    void patientsPayAnUnpaidBillOnline() throws Exception {
        String desk = api.staff("deskpay1", "RECEPTIONIST");
        Visit visit = visit("paypatient1", desk, doctor("drpay1", "500"), "MORNING");
        assertThat(api.get(visit.patient(), MY).at("/0/payOnline").asBoolean()).isTrue();

        JsonNode order = start(visit.patient(), visit.billId(), HttpStatus.OK);
        assertThat(order.get("keyId").asText()).isEqualTo(FakeRazorpay.KEY_ID);
        assertThat(order.get("amount").asLong()).as("in paise").isEqualTo(50000);
        assertThat(order.get("currency").asText()).isEqualTo("INR");
        assertThat(order.get("description").asText()).isEqualTo("Bill " + visit.billNumber());
        assertThat(order.at("/prefill/email").asText()).isEqualTo("paypatient1@example.com");
        String orderId = order.get("orderId").asText();
        assertThat(razorpay.orders.get(orderId)).isEqualTo(new FakeRazorpay.Order(orderId, 50000, visit.billNumber()));
        assertThat(startOrder(visit)).as("the unused order is reused").isEqualTo(orderId);
        assertThat(razorpay.calls).containsOnlyOnce("POST /v1/orders");

        FakeRazorpay.Checkout paid = razorpay.pay(orderId, "captured");
        confirm(visit.patient(), visit.billId(), orderId, paid.paymentId(), "0".repeat(64), HttpStatus.BAD_REQUEST);
        assertThat(atDesk(desk, visit).get("status").asText()).as("a wrong signature changes nothing").isEqualTo("UNPAID");

        JsonNode bill = confirm(visit, paid, HttpStatus.OK);
        assertThat(bill.get("status").asText()).isEqualTo("PAID");
        assertThat(bill.get("paymentMethod").asText()).isEqualTo("ONLINE");
        assertThat(bill.get("paymentReference").asText()).isEqualTo(paid.paymentId());
        assertThat(bill.get("payOnline").asBoolean()).isFalse();
        assertThat(confirm(visit, paid, HttpStatus.OK).get("status").asText()).as("confirmed twice").isEqualTo("PAID");
        assertThat(razorpay.refunds).isEmpty();
        assertThat(attempt(orderId)).containsEntry("status", "PAID").containsEntry("payment_id", paid.paymentId());

        JsonNode seenByDesk = atDesk(desk, visit);
        assertThat(seenByDesk.get("paymentMethod").asText()).isEqualTo("ONLINE");
        assertThat(seenByDesk.get("paidBy").isNull()).as("not taken at the desk").isTrue();
        assertThat(api.call(HttpMethod.GET, MY + "/" + visit.billId() + "/receipt.pdf", visit.patient(), null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(start(visit.patient(), visit.billId(), HttpStatus.CONFLICT).get("message").asText())
                .isEqualTo("This bill is already paid.");
    }

    @Test
    void anAuthorizedPaymentIsCapturedAndAFailedOneLeavesTheBillUnpaid() throws Exception {
        String desk = api.staff("deskpay2", "RECEPTIONIST");
        long doctorId = doctor("drpay2", "400");
        Visit authorized = visit("paypatient2", desk, doctorId, "EVENING");
        Visit failed = visit("paypatient3", desk, doctorId, "EVENING");

        FakeRazorpay.Checkout held = razorpay.pay(startOrder(authorized), "authorized");
        assertThat(confirm(authorized, held, HttpStatus.OK).get("status").asText()).isEqualTo("PAID");
        assertThat(razorpay.paymentStatus(held.paymentId())).isEqualTo("captured");
        assertThat(razorpay.calls).contains("POST /v1/payments/" + held.paymentId() + "/capture");

        String orderId = startOrder(failed);
        FakeRazorpay.Checkout declined = razorpay.pay(orderId, "failed");
        assertThat(confirm(failed, declined, HttpStatus.CONFLICT).get("message").asText())
                .isEqualTo("The payment didn't go through (failed). No money was taken; please try again.");
        assertThat(atDesk(desk, failed).get("status").asText()).isEqualTo("UNPAID");
        assertThat(api.get(failed.patient(), MY).at("/0/payOnline").asBoolean()).isTrue();
        // Paying again on the same order
        assertThat(startOrder(failed)).isEqualTo(orderId);
        assertThat(confirm(failed, razorpay.pay(orderId, "captured"), HttpStatus.OK).get("status").asText()).isEqualTo("PAID");
    }

    @Test
    void aPaymentThatCanNoLongerBeAppliedIsRefunded() throws Exception {
        String desk = api.staff("deskpay3", "RECEPTIONIST");
        long doctorId = doctor("drpay3", "500");
        Visit changed = visit("paypatient4", desk, doctorId, "MORNING");
        Visit cash = visit("paypatient5", desk, doctorId, "EVENING");
        Visit stuck = visit("paypatient6", desk, doctorId, "EVENING");

        // The desk adds an item after the patient started paying: the old order's payment is given back
        String oldOrder = startOrder(changed);
        expect(api.call(HttpMethod.POST, BILLS + "/" + changed.billId() + "/items", desk,
                "{\"description\":\"Dressing\",\"amount\":100}"), HttpStatus.OK);
        String newOrder = startOrder(changed);
        assertThat(newOrder).isNotEqualTo(oldOrder);
        assertThat(razorpay.orders.get(newOrder).amount()).isEqualTo(60000);
        FakeRazorpay.Checkout late = razorpay.pay(oldOrder, "captured");
        assertThat(confirm(changed, late, HttpStatus.CONFLICT).get("message").asText())
                .startsWith("The bill changed after the payment was started, so your payment of ₹500.00 is being refunded to you (refund rfnd_");
        assertThat(razorpay.refunds).containsExactly(late.paymentId() + " 50000");
        assertThat(attempt(oldOrder)).containsEntry("status", "REFUNDED").containsEntry("payment_id", late.paymentId());
        assertThat((String) attempt(oldOrder).get("refund_id")).startsWith("rfnd_");
        assertThat(atDesk(desk, changed).get("status").asText()).isEqualTo("UNPAID");
        assertThat(confirm(changed, razorpay.pay(newOrder, "captured"), HttpStatus.OK).get("status").asText()).isEqualTo("PAID");

        // Paid in cash at the desk meanwhile
        String cashOrder = startOrder(cash);
        expect(api.call(HttpMethod.POST, BILLS + "/" + cash.billId() + "/pay", desk, "{\"method\":\"CASH\"}"), HttpStatus.OK);
        FakeRazorpay.Checkout twice = razorpay.pay(cashOrder, "captured");
        assertThat(confirm(cash, twice, HttpStatus.CONFLICT).get("message").asText())
                .startsWith("This bill was already paid, so your payment of ₹500.00 is being refunded to you");
        assertThat(atDesk(desk, cash).get("paymentMethod").asText()).isEqualTo("CASH");

        // The refund itself fails: the patient is told to go to the desk, and it isn't tried again
        String stuckOrder = startOrder(stuck);
        expect(api.call(HttpMethod.POST, BILLS + "/" + stuck.billId() + "/pay", desk, "{\"method\":\"UPI\"}"), HttpStatus.OK);
        razorpay.refundsFail = true;
        FakeRazorpay.Checkout lost = razorpay.pay(stuckOrder, "captured");
        String message = confirm(stuck, lost, HttpStatus.CONFLICT).get("message").asText();
        assertThat(message).isEqualTo("This bill was already paid, and your payment of ₹500.00 couldn't be refunded "
                + "automatically. Please show the payment id " + lost.paymentId() + " at the front desk.");
        assertThat(attempt(stuckOrder)).containsEntry("status", "REFUND_FAILED");
        assertThat(confirm(stuck, lost, HttpStatus.CONFLICT).get("message").asText()).isEqualTo(message);
        assertThat(refundCalls(lost.paymentId())).isEqualTo(1);

        // The desk sees the payments that couldn't be applied: refunded, or to refund by hand
        JsonNode returned = atDesk(desk, changed).at("/unappliedPayments/0");
        assertThat(returned.get("paymentId").asText()).isEqualTo(late.paymentId());
        assertThat(returned.get("status").asText()).isEqualTo("REFUNDED");
        assertThat(returned.get("note").asText()).isEqualTo("The bill changed after the payment was started");
        assertThat(returned.get("refundId").asText()).startsWith("rfnd_");
        assertThat(atDesk(desk, changed).get("unappliedPayments")).hasSize(1);
        assertThat(atDesk(desk, stuck).at("/unappliedPayments/0/status").asText()).isEqualTo("REFUND_FAILED");
        assertThat(atDesk(desk, stuck).at("/unappliedPayments/0/amount").decimalValue()).isEqualByComparingTo("500");
    }

    @Test
    void onlyTheBillsPatientPaysAndTheDeskCantRecordOnlinePayments() throws Exception {
        String desk = api.staff("deskpay4", "RECEPTIONIST");
        Visit visit = visit("paypatient7", desk, doctor("drpay4", "500"), "MORNING");
        String other = api.registerPatient("payother");

        start(other, visit.billId(), HttpStatus.NOT_FOUND);
        FakeRazorpay.Checkout paid = razorpay.pay(startOrder(visit), "captured");
        confirm(other, visit.billId(), paid.orderId(), paid.paymentId(), paid.signature(), HttpStatus.NOT_FOUND);
        assertThat(api.call(HttpMethod.POST, MY + "/" + visit.billId() + "/online-payment", desk, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        expect(api.call(HttpMethod.POST, BILLS + "/" + visit.billId() + "/pay", desk, "{\"method\":\"online\"}"),
                HttpStatus.BAD_REQUEST);

        // A bill brought to nothing by a discount: nothing to pay online (Razorpay's smallest payment is Rs. 1)
        expect(api.call(HttpMethod.PUT, BILLS + "/" + visit.billId() + "/discount", desk, "{\"discount\":500}"), HttpStatus.OK);
        assertThat(api.get(visit.patient(), MY).at("/0/payOnline").asBoolean()).isFalse();
        assertThat(start(visit.patient(), visit.billId(), HttpStatus.CONFLICT).get("message").asText())
                .isEqualTo("There is nothing to pay on this bill.");
        expect(api.call(HttpMethod.PUT, BILLS + "/" + visit.billId() + "/discount", desk, "{\"discount\":0}"), HttpStatus.OK);

        // Razorpay can't be reached: a plain message, nothing changed
        expect(api.call(HttpMethod.POST, BILLS + "/" + visit.billId() + "/items", desk,
                "{\"description\":\"Dressing\",\"amount\":50}"), HttpStatus.OK);
        razorpay.down = true;
        assertThat(start(visit.patient(), visit.billId(), HttpStatus.BAD_GATEWAY).get("message").asText())
                .isEqualTo("The payment service couldn't be reached. Please try again in a moment.");
        assertThat(atDesk(desk, visit).get("status").asText()).isEqualTo("UNPAID");
    }

    // ------------------------------------------------------------------ webhook and refunds (step OP.2)

    @Test
    void razorpaysWebhookPaysABillTheAppNeverConfirmed() throws Exception {
        String desk = api.staff("deskpay5", "RECEPTIONIST");
        long doctorId = doctor("drpay5", "500");
        Visit lost = visit("paypatient8", desk, doctorId, "MORNING");
        Visit later = visit("paypatient9", desk, doctorId, "EVENING");

        FakeRazorpay.Checkout paid = razorpay.pay(startOrder(lost), "captured");
        assertThat(webhook("payment.captured", paid, null).getStatusCode()).as("no signature").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(webhook("payment.captured", paid, FakeRazorpay.KEY_SECRET).getStatusCode()).as("signed with another secret")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(atDesk(desk, lost).get("status").asText()).isEqualTo("UNPAID");

        assertThat(webhook("payment.captured", paid, FakeRazorpay.WEBHOOK_SECRET).getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode bill = atDesk(desk, lost);
        assertThat(bill.get("status").asText()).isEqualTo("PAID");
        assertThat(bill.get("paymentMethod").asText()).isEqualTo("ONLINE");
        assertThat(bill.get("paymentReference").asText()).isEqualTo(paid.paymentId());
        assertThat(webhook("payment.captured", paid, FakeRazorpay.WEBHOOK_SECRET).getStatusCode()).as("sent again").isEqualTo(HttpStatus.OK);
        assertThat(webhook("order.paid", paid, FakeRazorpay.WEBHOOK_SECRET).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirm(lost, paid, HttpStatus.OK).get("status").asText()).as("the app confirms afterwards").isEqualTo("PAID");
        assertThat(razorpay.refunds).isEmpty();

        // Other events and other orders are ignored
        FakeRazorpay.Checkout failed = razorpay.pay(startOrder(later), "failed");
        assertThat(webhook("payment.failed", failed, FakeRazorpay.WEBHOOK_SECRET).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(webhook("payment.captured", new FakeRazorpay.Checkout("order_not_ours", "pay_not_ours", ""),
                FakeRazorpay.WEBHOOK_SECRET).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(atDesk(desk, later).get("status").asText()).isEqualTo("UNPAID");

        // Razorpay can't be reached to check the payment: it is asked to send the event again
        FakeRazorpay.Checkout retried = razorpay.pay(failed.orderId(), "captured");
        razorpay.down = true;
        assertThat(webhook("payment.captured", retried, FakeRazorpay.WEBHOOK_SECRET).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(atDesk(desk, later).get("status").asText()).isEqualTo("UNPAID");
        razorpay.down = false;
        assertThat(webhook("payment.captured", retried, FakeRazorpay.WEBHOOK_SECRET).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(atDesk(desk, later).get("status").asText()).isEqualTo("PAID");
    }

    @Test
    void theDeskRefundsACancelledVisitPaidOnlineAndSeesPaymentsThatCameTooLate() throws Exception {
        String desk = api.staff("deskpay6", "RECEPTIONIST");
        long doctorId = doctor("drpay6", "500");
        Visit online = visit("paypatient10", desk, doctorId, "MORNING");
        Visit cash = visit("paypatient11", desk, doctorId, "EVENING");
        Visit tooLate = visit("paypatient12", desk, doctorId, "EVENING");
        String refundOnline = BILLS + "/" + online.billId() + "/refund-online";

        FakeRazorpay.Checkout paid = razorpay.pay(startOrder(online), "captured");
        confirm(online, paid, HttpStatus.OK);
        expect(api.call(HttpMethod.POST, refundOnline, desk, null), HttpStatus.CONFLICT); // not cancelled
        cancel(desk, online);
        assertThat(atDesk(desk, online).get("status").asText()).isEqualTo("REFUND_DUE");
        assertThat(api.call(HttpMethod.POST, refundOnline, online.patient(), null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        JsonNode refunded = expect(api.call(HttpMethod.POST, refundOnline, desk, null), HttpStatus.OK);
        assertThat(refunded.get("status").asText()).isEqualTo("REFUNDED");
        assertThat(refunded.get("refundMethod").asText()).isEqualTo("ONLINE");
        assertThat(refunded.get("refundReference").asText()).startsWith("rfnd_");
        assertThat(refunded.get("refundedBy").asText()).isEqualTo("deskpay6");
        assertThat(razorpay.refunds).containsExactly(paid.paymentId() + " 50000");
        expect(api.call(HttpMethod.POST, refundOnline, desk, null), HttpStatus.CONFLICT);
        assertThat(api.get(online.patient(), MY).at("/0/refundMethod").asText()).isEqualTo("ONLINE");

        // Paid in cash: refunded at the desk, not online
        expect(api.call(HttpMethod.POST, BILLS + "/" + cash.billId() + "/pay", desk, "{\"method\":\"CASH\"}"), HttpStatus.OK);
        cancel(desk, cash);
        assertThat(expect(api.call(HttpMethod.POST, BILLS + "/" + cash.billId() + "/refund-online", desk, null),
                HttpStatus.CONFLICT).get("message").asText())
                .isEqualTo("This bill was paid at the desk: record the refund given at the desk instead");

        // The visit is cancelled while the patient pays: the payment is given back, and the desk sees why
        String order = startOrder(tooLate);
        cancel(desk, tooLate);
        FakeRazorpay.Checkout landed = razorpay.pay(order, "captured");
        assertThat(webhook("payment.captured", landed, FakeRazorpay.WEBHOOK_SECRET).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(razorpay.refunds).contains(landed.paymentId() + " 50000");
        JsonNode seen = atDesk(desk, tooLate);
        assertThat(seen.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(seen.at("/unappliedPayments/0/paymentId").asText()).isEqualTo(landed.paymentId());
        assertThat(seen.at("/unappliedPayments/0/status").asText()).isEqualTo("REFUNDED");
        assertThat(seen.at("/unappliedPayments/0/note").asText()).isEqualTo("This bill was cancelled");
    }
}
