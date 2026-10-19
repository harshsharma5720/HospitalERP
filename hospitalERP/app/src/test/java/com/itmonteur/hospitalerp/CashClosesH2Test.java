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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Closing the day at the front desk (docs/DAILY_CASH_SUMMARY_PLAN.md). Same configuration as FeatureFlowH2Test, so
 * both share one application context and database. Other tests take cash today too, so the first close's amounts are
 * read from the summary; after it, only this test's payments count. No other test closes a day. The phone numbers
 * 900006(84-85)xx and the accounts *close* are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class CashClosesH2Test {

    private static final String BILLS = "/api/receptionist/bills";
    private static final String SUMMARY = "/api/receptionist/cash-summary";
    private static final String CLOSES = "/api/receptionist/cash-closes";

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    private void ok(String token, HttpMethod method, String url, String body) {
        assertThat(api.call(method, url, token, body).getStatusCode()).as(url).isEqualTo(HttpStatus.OK);
    }

    // Three visits with a new doctor (fee 500), checked in today; their bills' ids
    private List<Long> billsToday(String desk, String doctor, String surname, String phonePrefix) throws Exception {
        long doctorId = api.doctorWithSchedule(doctor).id();
        ok(api.admin(), HttpMethod.PUT, "/api/admin/fees/" + doctorId, "{\"consultationFee\":500}");
        List<Long> billIds = new ArrayList<>();
        for (long appointmentId : api.visitsToday(desk, doctorId, surname, phonePrefix, jdbc)) {
            ok(desk, HttpMethod.POST, "/api/receptionist/appointments/" + appointmentId + "/check-in", null);
            billIds.add(api.get(desk, BILLS + "/by-appointment/" + appointmentId).get("id").asLong());
        }
        return billIds;
    }

    private ResponseEntity<String> close(String token, String counted, String note) {
        return api.call(HttpMethod.POST, CLOSES, token, "{\"counted\":" + counted
                + (note == null ? "" : ",\"note\":\"" + note + "\"") + "}");
    }

    private JsonNode closed(String token, String counted, String note) throws Exception {
        ResponseEntity<String> res = close(token, counted, note);
        assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody());
    }

    private static void money(JsonNode node, String field, String expected) {
        assertThat(node.get(field).decimalValue()).as(field).isEqualByComparingTo(expected);
    }

    private static LocalDateTime time(JsonNode node, String field) {
        return LocalDateTime.parse(node.get(field).asText());
    }

    @Test
    void closingTheDayCountsTheCashSinceTheLastClose() throws Exception {
        String desk = api.staff("deskclose", "RECEPTIONIST");
        List<Long> bills = new ArrayList<>(billsToday(desk, "drclose1", "Closeone", "90000684"));
        bills.addAll(billsToday(desk, "drclose2", "Closetwo", "90000685"));

        // No close yet: the first covers today, with whatever cash was taken today
        ok(desk, HttpMethod.POST, BILLS + "/" + bills.get(0) + "/pay", "{\"method\":\"CASH\"}");
        JsonNode since = api.get(desk, SUMMARY).get("sinceLastClose");
        assertThat(time(since, "from")).isEqualTo(LocalDate.now().atStartOfDay());
        assertThat(since.get("cashIn").decimalValue()).isGreaterThanOrEqualTo(new BigDecimal("500"));
        BigDecimal expected = since.get("expected").decimalValue();
        assertThat(expected).isEqualByComparingTo(since.get("cashIn").decimalValue().subtract(since.get("cashOut").decimalValue()));

        // Counted as expected: no note needed
        JsonNode first = closed(desk, expected.toPlainString(), null);
        assertThat(time(first, "periodStart")).isEqualTo(LocalDate.now().atStartOfDay());
        assertThat(first.get("closedBy").asText()).isEqualTo("deskclose");
        money(first, "expected", expected.toPlainString());
        money(first, "counted", expected.toPlainString());
        money(first, "difference", "0");
        assertThat(first.get("note").isNull()).isTrue();

        // After it: nothing yet; then cash 500 paid and refunded, cash 600, and a card payment that isn't in the drawer
        since = api.get(desk, SUMMARY).get("sinceLastClose");
        assertThat(time(since, "from")).isEqualTo(time(first, "closedAt"));
        assertThat(since.get("payments").asInt()).isZero();
        money(since, "expected", "0");
        ok(desk, HttpMethod.POST, BILLS + "/" + bills.get(1) + "/pay", "{\"method\":\"CASH\"}");
        long cancelled = api.get(desk, BILLS + "/" + bills.get(1)).get("appointmentId").asLong();
        ok(desk, HttpMethod.DELETE, "/api/receptionist/deleteAppointment/" + cancelled, null);
        ok(desk, HttpMethod.POST, BILLS + "/" + bills.get(1) + "/refund", "{\"method\":\"CASH\"}");
        ok(desk, HttpMethod.POST, BILLS + "/" + bills.get(2) + "/items", "{\"description\":\"Dressing\",\"amount\":100}");
        ok(desk, HttpMethod.POST, BILLS + "/" + bills.get(2) + "/pay", "{\"method\":\"CASH\"}");
        ok(desk, HttpMethod.POST, BILLS + "/" + bills.get(3) + "/pay", "{\"method\":\"CARD\"}");
        since = api.get(desk, SUMMARY).get("sinceLastClose");
        assertThat(since.get("payments").asInt()).isEqualTo(2);
        money(since, "cashIn", "1100");
        assertThat(since.get("refunds").asInt()).isEqualTo(1);
        money(since, "cashOut", "500");
        money(since, "expected", "600");

        // A different count needs a note; the difference is counted - expected
        ResponseEntity<String> noNote = close(desk, "550", null);
        assertThat(noNote.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(noNote.getBody()).contains("Please say why");
        assertThat(close(desk, "550", "   ").getStatusCode()).as("a blank note").isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode second = closed(desk, "550", "Gave Rs. 50 too much change");
        assertThat(time(second, "periodStart")).isEqualTo(time(first, "closedAt"));
        money(second, "cashIn", "1100");
        money(second, "cashOut", "500");
        money(second, "expected", "600");
        money(second, "counted", "550");
        money(second, "difference", "-50");
        assertThat(second.get("note").asText()).isEqualTo("Gave Rs. 50 too much change");

        // In the audit log, with the amounts
        assertThat(jdbc.queryForList("SELECT CONCAT_WS(' ', actor_username, details) FROM audit_log WHERE action = "
                        + "'CASH_DAY_CLOSED' AND target_id = ?", String.class, second.get("id").asLong()))
                .containsExactly("deskclose expected Rs. 600.00, counted Rs. 550.00, difference Rs. -50.00");

        // The day's closes, for the desk and for admins, and a range's row
        assertThat(api.get(desk, SUMMARY).get("closes")).extracting(c -> c.get("id").asLong())
                .containsExactly(first.get("id").asLong(), second.get("id").asLong());
        String admin = api.admin();
        LocalDate today = LocalDate.now();
        JsonNode adminDay = api.get(admin, "/api/admin/cash-summary?date=" + today);
        assertThat(adminDay.get("closes")).hasSize(2);
        assertThat(adminDay.get("sinceLastClose").isNull()).as("today: shown to admins too").isFalse();
        assertThat(api.get(admin, "/api/admin/cash-summary?date=" + today.minusDays(1)).get("sinceLastClose").isNull())
                .as("only for today").isTrue();
        JsonNode range = api.get(admin, "/api/admin/cash-summary/range?from=" + today.minusDays(1) + "&to=" + today);
        assertThat(range.get("days").get(0).get("closes").asInt()).isZero();
        assertThat(range.get("days").get(0).get("difference").isNull()).isTrue();
        assertThat(range.get("days").get(1).get("closes").asInt()).isEqualTo(2);
        money(range.get("days").get(1), "difference", "-50");
        assertThat(range.get("closes")).hasSize(2);

        // Someone else closed the same cash a moment ago: refused
        jdbc.update("INSERT INTO cash_closes (period_start, closed_at, closed_by, cash_in, cash_out, cash_expected, "
                        + "cash_counted, cash_difference) VALUES (?, ?, 'deskclose', 0, 0, 0, 0, 0)",
                time(second, "closedAt"), time(second, "closedAt"));
        ResponseEntity<String> twice = close(desk, "0", null);
        assertThat(twice.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(twice.getBody()).contains("Someone has just closed the day");
        jdbc.update("DELETE FROM cash_closes WHERE period_start = ? AND closed_at = ?", time(second, "closedAt"),
                time(second, "closedAt"));

        // An admin at the front desk closes too
        JsonNode byAdmin = closed(admin, "0", null);
        assertThat(byAdmin.get("closedBy").asText()).isEqualTo("flowadmin");
        assertThat(time(byAdmin, "periodStart")).isEqualTo(time(second, "closedAt"));
    }

    @Test
    void wrongAmountsAndOtherRolesAreRefused() {
        String desk = api.staff("deskclose2", "RECEPTIONIST");
        assertThat(api.call(HttpMethod.POST, CLOSES, desk, "{}").getStatusCode()).as("no amount").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(close(desk, "-1", "x").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(close(desk, "10.005", "x").getStatusCode()).as("3 decimals").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(close(desk, "10000000.01", "x").getStatusCode()).as("over a crore").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(close(desk, "1", "x".repeat(501)).getStatusCode()).as("a long note").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(close(api.registerPatient("closepatient"), "0", null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(close(null, "0", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cash_closes WHERE closed_by IN ('deskclose2', 'closepatient')",
                Integer.class)).as("nothing closed").isZero();
    }
}
