package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bills and payments in the notification centre (docs/NOTIFICATION_CENTRE_PLAN.md, NC.3) at the front desk; online
 * payments are in OnlinePaymentH2Test. Same configuration as FeatureFlowH2Test, so both share one application context;
 * the accounts *inboxbill* and the phone numbers 9000069(2-3)xx are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class InboxBillsH2Test {

    private static final String BILLS = "/api/receptionist/bills";

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    private long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    private List<String> messages(long userId) {
        return jdbc.queryForList("SELECT CONCAT(kind, ' ', title, ' | ', body, ' -> ', COALESCE(link, '')) FROM inbox_messages "
                + "WHERE user_id = ? ORDER BY id", String.class, userId);
    }

    private void ok(String token, HttpMethod method, String url, String body) {
        assertThat(api.call(method, url, token, body).getStatusCode()).as(url).isEqualTo(HttpStatus.OK);
    }

    @Test
    void aBillMadePaidCancelledAndRefundedReachesThePatientAndTheRefundTheDesk() throws Exception {
        String desk = api.staff("deskinboxbill", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drinboxbill").id();
        ok(api.admin(), HttpMethod.PUT, "/api/admin/fees/" + doctorId, "{\"consultationFee\":1250}");
        String patient = api.registerPatient("inboxbillpatient");
        long patientUser = userId("inboxbillpatient");
        long visit = api.bookedForToday(patient, api.freeSlotsTomorrow(patient, doctorId, "MORNING").get(0).get("id").asLong(),
                null, jdbc);
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH));
        int before = messages(patientUser).size(); // the booking's

        ok(desk, HttpMethod.POST, "/api/receptionist/appointments/" + visit + "/check-in", null);
        JsonNode bill = api.get(desk, BILLS + "/by-appointment/" + visit);
        long billId = bill.get("id").asLong();
        String number = bill.get("billNumber").asText();
        assertThat(messages(patientUser)).hasSize(before + 1).last().isEqualTo("BILL_READY Your bill is ready | " + number
                + " for the visit with Dr. drinboxbill on " + today + ": ₹1,250.00. Pay at the front desk, or online in My account."
                + " -> /edit-profile");

        ok(desk, HttpMethod.POST, BILLS + "/" + billId + "/pay", "{\"method\":\"UPI\",\"reference\":\"UPI-9\"}");
        assertThat(messages(patientUser)).last().isEqualTo("PAYMENT_RECEIVED Payment received | ₹1,250.00 for " + number
                + ", by UPI. The receipt is in My account. -> /edit-profile");

        // Cancelled after paying: the patient hears the refund is due, every receptionist that there is one to give
        ok(desk, HttpMethod.DELETE, "/api/receptionist/deleteAppointment/" + visit, null);
        assertThat(messages(patientUser)).contains("REFUND_DUE Refund due | The visit with Dr. drinboxbill on " + today
                + " was cancelled: ₹1,250.00 for " + number + " will be given back. -> /edit-profile");
        assertThat(messages(userId("deskinboxbill"))).containsExactly("REFUND_DUE Refund due: " + number + " | inboxbillpatient, "
                + "₹1,250.00, paid by UPI. The visit with Dr. drinboxbill on " + today + " was cancelled. -> /bills/" + billId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inbox_messages WHERE user_id = ? AND title = ?", Integer.class,
                userId("flowadmin"), "Refund due: " + number)).as("the front desk's, not the admins'").isZero();

        ok(desk, HttpMethod.POST, BILLS + "/" + billId + "/refund", "{\"method\":\"CASH\"}");
        assertThat(messages(patientUser)).last().isEqualTo("REFUND_GIVEN Refund given | ₹1,250.00 for " + number
                + ", by cash. -> /edit-profile");

        // A walk-in without a login: the bill reaches nobody
        api.staff("deskinboxbill2", "RECEPTIONIST");
        long walkInDoctor = api.doctorWithSchedule("drinboxbill2").id();
        long walkIn = api.visitsToday(desk, walkInDoctor, "Inboxbillwalk", "90000692", jdbc).get(0);
        int deskBefore = messages(userId("deskinboxbill2")).size();
        ok(desk, HttpMethod.POST, "/api/receptionist/appointments/" + walkIn + "/check-in", null);
        long walkInBill = api.get(desk, BILLS + "/by-appointment/" + walkIn).get("id").asLong();
        ok(desk, HttpMethod.POST, BILLS + "/" + walkInBill + "/pay", "{\"method\":\"CASH\"}");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inbox_messages WHERE body LIKE ?", Integer.class,
                "%" + api.get(desk, BILLS + "/" + walkInBill).get("billNumber").asText() + "%")).isZero();
        // ... until it is due for refund: then the desk is told
        ok(desk, HttpMethod.DELETE, "/api/receptionist/deleteAppointment/" + walkIn, null);
        assertThat(messages(userId("deskinboxbill2"))).hasSize(deskBefore + 1).last().asString()
                .startsWith("REFUND_DUE Refund due: B-").contains("Inboxbillwalk 1, ₹0.00, paid by cash.");
    }
}
