package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The daily cash summary (docs/DAILY_CASH_SUMMARY_PLAN.md) from bills paid and refunded at the desk. Same
 * configuration as FeatureFlowH2Test, so both share one application context and database: the payments are moved to
 * days in March 2021, which no other test uses, and today's are compared before and after. The phone numbers
 * 900006(81-83)xx and the accounts *cash* are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class CashSummaryH2Test {

    private static final String BILLS = "/api/receptionist/bills";
    private static final String DESK_SUMMARY = "/api/receptionist/cash-summary";
    private static final String ADMIN_SUMMARY = "/api/admin/cash-summary";
    private static final LocalDate DAY = LocalDate.of(2021, 3, 15);

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

    private HttpStatus status(String token, String url) {
        return HttpStatus.valueOf(api.call(HttpMethod.GET, url, token, null).getStatusCode().value());
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

    private String billNumber(String desk, long billId) throws Exception {
        return api.get(desk, BILLS + "/" + billId).get("billNumber").asText();
    }

    private ResponseEntity<byte[]> pdf(String token, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    // The PDF's text; it must be a PDF named day-summary-{day}.pdf
    private static String pdfText(ResponseEntity<byte[]> res, LocalDate day) throws Exception {
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(res.getHeaders().getContentDisposition().getFilename()).isEqualTo("day-summary-" + day + ".pdf");
        byte[] body = res.getBody();
        assertThat(new String(body, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(body);
        StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(new PdfTextExtractor(reader).getTextFromPage(page)).append('\n');
        }
        return text.toString();
    }

    private static void money(JsonNode node, String field, String expected) {
        assertThat(node.get(field).decimalValue()).as(field).isEqualByComparingTo(expected);
    }

    private static void method(JsonNode method, String name, int payments, String taken, int refunds, String refunded,
                               String net) {
        assertThat(method.get("method").asText()).isEqualTo(name);
        assertThat(method.get("payments").asInt()).as(name + " payments").isEqualTo(payments);
        money(method, "taken", taken);
        assertThat(method.get("refunds").asInt()).as(name + " refunds").isEqualTo(refunds);
        money(method, "refunded", refunded);
        money(method, "net", net);
    }

    @Test
    void aDaysPaymentsAndRefundsPerMethodAndPersonAndARange() throws Exception {
        String desk1 = api.staff("deskcash1", "RECEPTIONIST");
        String desk2 = api.staff("deskcash2", "RECEPTIONIST");
        List<Long> bills = new ArrayList<>(billsToday(desk1, "drcash1", "Cashone", "90000681"));
        bills.addAll(billsToday(desk1, "drcash2", "Cashtwo", "90000682"));
        long cash = bills.get(0), card = bills.get(1), upi = bills.get(2), refunded = bills.get(3);
        long online = bills.get(4), unpaid = bills.get(5);

        ok(desk1, HttpMethod.POST, BILLS + "/" + cash + "/items", "{\"description\":\"Dressing\",\"amount\":100}");
        ok(desk1, HttpMethod.POST, BILLS + "/" + cash + "/pay", "{\"method\":\"CASH\"}"); // 600
        ok(desk1, HttpMethod.POST, BILLS + "/" + card + "/pay", "{\"method\":\"CARD\"}");
        ok(desk2, HttpMethod.POST, BILLS + "/" + upi + "/pay", "{\"method\":\"UPI\",\"reference\":\"UPI-77\"}");
        ok(desk2, HttpMethod.POST, BILLS + "/" + refunded + "/pay", "{\"method\":\"CASH\"}");
        long refundedVisit = api.get(desk1, BILLS + "/" + refunded).get("appointmentId").asLong();
        ok(desk1, HttpMethod.DELETE, "/api/receptionist/deleteAppointment/" + refundedVisit, null); // refund due
        ok(desk1, HttpMethod.POST, BILLS + "/" + refunded + "/refund", "{\"method\":\"CASH\"}");
        // Paid online through Razorpay, as OnlinePaymentService records it: no one at the desk
        jdbc.update("UPDATE bills SET status = 'PAID', payment_method = 'ONLINE', paid_by = NULL WHERE id = ?", online);

        // Onto days in March 2021: the card payment at the day's first moment, the UPI one at the next day's;
        // the refunded visit was paid on the day and refunded at the next day's first moment
        jdbc.update("UPDATE bills SET paid_at = ? WHERE id = ?", DAY.atTime(9, 0), cash);
        jdbc.update("UPDATE bills SET paid_at = ? WHERE id = ?", DAY.atStartOfDay(), card);
        jdbc.update("UPDATE bills SET paid_at = ? WHERE id = ?", DAY.plusDays(1).atStartOfDay(), upi);
        jdbc.update("UPDATE bills SET paid_at = ?, refunded_at = ? WHERE id = ?", DAY.atTime(18, 0),
                DAY.plusDays(1).atStartOfDay(), refunded);
        jdbc.update("UPDATE bills SET paid_at = ? WHERE id = ?", DAY.atTime(11, 0), online);
        jdbc.update("UPDATE bills SET visit_date = ? WHERE id = ?", DAY, unpaid);

        String admin = api.admin();
        JsonNode day = api.get(admin, ADMIN_SUMMARY + "?date=" + DAY);
        assertThat(day.get("date").asText()).isEqualTo(DAY.toString());
        JsonNode methods = day.get("methods");
        assertThat(methods).hasSize(4);
        method(methods.get(0), "CASH", 2, "1100", 0, "0", "1100");
        method(methods.get(1), "CARD", 1, "500", 0, "0", "500");
        method(methods.get(2), "UPI", 0, "0", 0, "0", "0");
        method(methods.get(3), "ONLINE", 1, "500", 0, "0", "500");
        JsonNode totals = day.get("totals");
        assertThat(totals.get("payments").asInt()).isEqualTo(4);
        money(totals, "taken", "2100");
        assertThat(totals.get("refunds").asInt()).isZero();
        money(totals, "refunded", "0");
        money(totals, "net", "2100");

        // Who took it: the desk by name, the online payment last (no one)
        JsonNode people = day.get("people");
        assertThat(people).extracting(person -> person.get("by").asText(null)).containsExactly("deskcash1", "deskcash2", null);
        assertThat(people.get(0).get("payments").asInt()).isEqualTo(2);
        money(people.get(0), "taken", "1100");
        money(people.get(1), "taken", "500");
        money(people.get(2), "taken", "500");

        // The lines in the order they happened
        JsonNode lines = day.get("lines");
        assertThat(lines).extracting(line -> line.get("kind").asText() + " " + line.get("billId").asLong() + " "
                        + line.get("method").asText())
                .containsExactly("PAYMENT " + card + " CARD", "PAYMENT " + cash + " CASH", "PAYMENT " + online + " ONLINE",
                        "PAYMENT " + refunded + " CASH");
        JsonNode first = lines.get(1);
        assertThat(first.get("at").asText()).startsWith(DAY + "T09:00");
        assertThat(first.get("billNumber").asText()).isEqualTo(billNumber(desk1, cash));
        assertThat(first.get("patientName").asText()).isEqualTo("Cashone 1");
        assertThat(first.get("by").asText()).isEqualTo("deskcash1");
        money(first, "amount", "600");

        // The day's visits still to pay
        assertThat(day.get("unpaid").get("bills").asInt()).isEqualTo(1);
        money(day.get("unpaid"), "amount", "500");

        // The next day: the UPI payment with its reference, and the refund of the visit paid the day before
        JsonNode next = api.get(admin, ADMIN_SUMMARY + "?date=" + DAY.plusDays(1));
        assertThat(next.get("lines")).extracting(line -> line.get("kind").asText() + " " + line.get("billId").asLong())
                .containsExactly("PAYMENT " + upi, "REFUND " + refunded);
        assertThat(next.get("lines").get(0).get("reference").asText()).isEqualTo("UPI-77");
        assertThat(next.get("lines").get(1).get("by").asText()).as("refunded by").isEqualTo("deskcash1");
        method(next.get("methods").get(0), "CASH", 0, "0", 1, "500", "-500");
        assertThat(next.get("people")).extracting(person -> person.get("by").asText()).containsExactly("deskcash1", "deskcash2");
        money(next.get("totals"), "net", "0");

        // Three days: a row per day (the first without money) with the cash's own net, and the whole range
        JsonNode range = api.get(admin, ADMIN_SUMMARY + "/range?from=" + DAY.minusDays(1) + "&to=" + DAY.plusDays(1));
        JsonNode days = range.get("days");
        assertThat(days).extracting(d -> d.get("date").asText())
                .containsExactly(DAY.minusDays(1).toString(), DAY.toString(), DAY.plusDays(1).toString());
        money(days.get(0), "taken", "0");
        money(days.get(0), "cashNet", "0");
        money(days.get(1), "net", "2100");
        money(days.get(1), "cashNet", "1100");
        money(days.get(2), "refunded", "500");
        money(days.get(2), "cashNet", "-500");
        assertThat(range.get("totals").get("payments").asInt()).isEqualTo(5);
        money(range.get("totals"), "taken", "2600");
        money(range.get("totals"), "net", "2100");
        method(range.get("methods").get(0), "CASH", 2, "1100", 1, "500", "600");
        method(range.get("methods").get(2), "UPI", 1, "500", 0, "0", "500");

        // The day as a PDF, for admins
        assertThat(pdfText(pdf(admin, ADMIN_SUMMARY + "/pdf?date=" + DAY), DAY))
                .contains("Day summary: Monday, 15 Mar 2021", "deskcash1", "Online (the patient)", "Rs. 2,100.00", "Cashone 1");
    }

    @Test
    void theDeskSeesTodayAdminsAnyDayUpToThirtyOneDays() throws Exception {
        String desk = api.staff("deskcash3", "RECEPTIONIST");
        long bill = billsToday(desk, "drcash3", "Cashthree", "90000683").get(0);
        JsonNode before = api.get(desk, DESK_SUMMARY);
        ok(desk, HttpMethod.POST, BILLS + "/" + bill + "/pay", "{\"method\":\"CASH\"}");
        JsonNode after = api.get(desk, DESK_SUMMARY);

        LocalDate today = LocalDate.now();
        assertThat(after.get("date").asText()).isEqualTo(today.toString());
        JsonNode cashBefore = before.get("methods").get(0), cashAfter = after.get("methods").get(0);
        assertThat(cashAfter.get("payments").asInt()).isEqualTo(cashBefore.get("payments").asInt() + 1);
        assertThat(cashAfter.get("taken").decimalValue().subtract(cashBefore.get("taken").decimalValue()))
                .isEqualByComparingTo("500");
        assertThat(after.get("lines")).anySatisfy(line -> {
            assertThat(line.get("billId").asLong()).isEqualTo(bill);
            assertThat(line.get("by").asText()).isEqualTo("deskcash3");
        });
        assertThat(after.get("people")).anySatisfy(person -> {
            assertThat(person.get("by").asText()).isEqualTo("deskcash3");
            money(person, "taken", "500");
        });
        String admin = api.admin();
        assertThat(api.get(admin, ADMIN_SUMMARY + "?date=" + today).get("totals").get("taken").decimalValue())
                .as("the admin's today is the desk's").isEqualByComparingTo(after.get("totals").get("taken").decimalValue());
        assertThat(status(admin, DESK_SUMMARY)).as("admins at the front desk").isEqualTo(HttpStatus.OK);

        // Who may see it
        assertThat(status(api.registerPatient("cashpatient"), DESK_SUMMARY)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(api.login("drcash3", "doctor-123"), DESK_SUMMARY)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(null, DESK_SUMMARY)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(status(desk, ADMIN_SUMMARY + "?date=" + today)).as("the desk: today only").isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(desk, ADMIN_SUMMARY + "/range?from=" + today + "&to=" + today)).isEqualTo(HttpStatus.FORBIDDEN);

        // Today's PDF for the desk; any day's only for admins
        assertThat(pdfText(pdf(desk, DESK_SUMMARY + "/today.pdf"), today)).contains("deskcash3");
        assertThat(pdf(desk, ADMIN_SUMMARY + "/pdf?date=" + today).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(pdf(api.login("drcash3", "doctor-123"), DESK_SUMMARY + "/today.pdf").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(pdf(admin, ADMIN_SUMMARY + "/pdf?date=" + today.plusDays(1)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Which days
        assertThat(status(admin, ADMIN_SUMMARY + "?date=" + today.plusDays(1))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status(admin, ADMIN_SUMMARY)).as("no day").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status(admin, ADMIN_SUMMARY + "?date=yesterday")).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status(admin, ADMIN_SUMMARY + "/range?from=" + today + "&to=" + today.minusDays(1)))
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status(admin, ADMIN_SUMMARY + "/range?from=" + today.minusDays(31) + "&to=" + today))
                .as("32 days").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status(admin, ADMIN_SUMMARY + "/range?from=" + today.minusDays(30) + "&to=" + today))
                .as("31 days").isEqualTo(HttpStatus.OK);
        assertThat(status(admin, ADMIN_SUMMARY + "/range?from=" + today.minusDays(1) + "&to=" + today.plusDays(1)))
                .as("into the future").isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode empty = api.get(admin, ADMIN_SUMMARY + "?date=" + DAY.minusDays(10));
        assertThat(empty.get("lines")).isEmpty();
        assertThat(empty.get("people")).isEmpty();
        money(empty.get("totals"), "net", "0");
    }
}
