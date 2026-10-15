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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Billing (docs/BILLING_PLAN.md): consultation fees per doctor (step B.1), bills made and changed by the queue
 * (step B.2), the front desk's billing API (step B.3), the patient's own bills (docs/PATIENT_BILLS_PLAN.md). Same configuration as FeatureFlowH2Test, so both share one application context; the phone numbers
 * 9000055xxx are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class BillingH2Test {

    private static final String FEES = "/api/admin/fees";

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;
    private int batch = 0;

    @BeforeEach
    void setUpApi() {
        api = new TestApi(rest, json);
    }

    private JsonNode feeOf(String token, long doctorId) throws Exception {
        for (JsonNode d : api.get(token, FEES).get("doctors")) {
            if (d.get("doctorId").asLong() == doctorId) {
                return d;
            }
        }
        throw new AssertionError("doctor " + doctorId + " not in the fee list");
    }

    private ResponseEntity<String> setFee(String token, long doctorId, String amount) {
        return api.call(HttpMethod.PUT, FEES + "/" + doctorId, token, "{\"consultationFee\":" + amount + "}");
    }

    @Test
    void adminsSetEachDoctorsConsultationFee() throws Exception {
        String admin = api.admin();
        TestApi.Doctor doctor = api.doctorWithSchedule("drfees");

        JsonNode fees = api.get(admin, FEES);
        assertThat(fees.get("defaultFee").asDouble()).as("BILLING_DEFAULT_FEE is not set").isZero();
        JsonNode mine = feeOf(admin, doctor.id());
        assertThat(mine.get("doctorName").asText()).as("the user name while the profile has none").isEqualTo("drfees");
        assertThat(mine.get("consultationFee").isNull()).as("no fee yet").isTrue();
        assertThat(mine.get("active").asBoolean()).isTrue();

        ResponseEntity<String> set = setFee(admin, doctor.id(), "500");
        assertThat(set.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(set.getBody()).contains("\"consultationFee\":500.00");
        assertThat(feeOf(admin, doctor.id()).get("consultationFee").asDouble()).isEqualTo(500.0);
        assertThat(setFee(admin, doctor.id(), "750.5").getBody()).contains("\"consultationFee\":750.50");
        assertThat(setFee(admin, doctor.id(), "0").getStatusCode()).as("free consultations").isEqualTo(HttpStatus.OK);

        // A deactivated doctor stays in the list, marked
        api.call(HttpMethod.PUT, "/api/admin/users/" + doctor.userId() + "/deactivate", admin, null);
        assertThat(feeOf(admin, doctor.id()).get("active").asBoolean()).isFalse();
    }

    @Test
    void wrongFeesAndOtherRolesAreRefused() throws Exception {
        String admin = api.admin();
        long doctorId = api.doctorWithSchedule("drfees2").id();

        for (String wrong : List.of("-1", "100000.01", "12.345", "null")) {
            ResponseEntity<String> res = setFee(admin, doctorId, wrong);
            assertThat(res.getStatusCode()).as(wrong).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(setFee(admin, doctorId, "100000").getStatusCode()).as("the highest fee").isEqualTo(HttpStatus.OK);
        assertThat(setFee(admin, 987654, "500").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        for (String token : List.of(api.staff("deskfees", "RECEPTIONIST"), api.login("drfees2", "doctor-123"),
                api.registerPatient("feespeek"))) {
            assertThat(api.call(HttpMethod.GET, FEES, token, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(setFee(token, doctorId, "1").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ------------------------------------------------------------------ bills made by the queue (step B.2)

    // Three visits today (10:00, 16:00, 16:30), see TestApi.visitsToday
    private List<Long> visitsToday(String desk, long doctorId, String surname) throws Exception {
        return api.visitsToday(desk, doctorId, surname, "9000055" + (++batch), jdbc);
    }

    private Map<String, Object> billOf(long appointmentId) {
        return jdbc.queryForMap("SELECT * FROM bills WHERE appointment_id = ?", appointmentId);
    }

    private String statusOf(long appointmentId) {
        return (String) billOf(appointmentId).get("status");
    }

    private int billsFor(long appointmentId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM bills WHERE appointment_id = ?", Integer.class, appointmentId);
    }

    private List<Map<String, Object>> itemsOf(long appointmentId) {
        return jdbc.queryForList("SELECT i.* FROM bill_items i JOIN bills b ON b.id = i.bill_id "
                + "WHERE b.appointment_id = ? ORDER BY i.id", appointmentId);
    }

    // Payment at the desk comes with step B.3; until then a bill is marked paid in the database
    private void markPaid(long appointmentId) {
        jdbc.update("UPDATE bills SET status = 'PAID' WHERE appointment_id = ?", appointmentId);
    }

    private void checkIn(String desk, long appointmentId) {
        assertThat(api.call(HttpMethod.POST, "/api/receptionist/appointments/" + appointmentId + "/check-in", desk, null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void undoCheckIn(String desk, long appointmentId) {
        assertThat(api.call(HttpMethod.DELETE, "/api/receptionist/appointments/" + appointmentId + "/check-in", desk, null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void cancel(String desk, long appointmentId) {
        assertThat(api.call(HttpMethod.DELETE, "/api/receptionist/deleteAppointment/" + appointmentId, desk, null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void checkingInMakesTheBillWithTheDoctorsFee() throws Exception {
        String desk = api.staff("deskbill", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drbill").id();
        setFee(api.admin(), doctorId, "600");
        List<Long> visits = visitsToday(desk, doctorId, "Billington");
        assertThat(billsFor(visits.get(0))).as("booked, not arrived: no bill yet").isZero();

        checkIn(desk, visits.get(0));

        Map<String, Object> bill = billOf(visits.get(0));
        assertThat(bill.get("status")).isEqualTo("UNPAID");
        assertThat((String) bill.get("bill_number")).matches("B-" + LocalDate.now().getYear() + "-\\d{6}");
        assertThat(bill.get("patient_name")).isEqualTo("Billington 1");
        assertThat(bill.get("doctor_name")).isEqualTo("drbill");
        assertThat(bill.get("visit_date").toString()).isEqualTo(LocalDate.now().toString());
        assertThat((BigDecimal) bill.get("discount")).isEqualByComparingTo("0");
        List<Map<String, Object>> items = itemsOf(visits.get(0));
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("description")).isEqualTo("Consultation — Dr. drbill");
        assertThat((BigDecimal) items.get(0).get("amount")).isEqualByComparingTo("600");
        assertThat(items.get(0).get("consultation")).isEqualTo(true);

        checkIn(desk, visits.get(0));
        assertThat(billsFor(visits.get(0))).as("checking in twice: still one bill").isEqualTo(1);

        // A doctor without a fee of their own: the default fee (0 here)
        long freeDoctor = api.doctorWithSchedule("drbillfree").id();
        long freeVisit = visitsToday(desk, freeDoctor, "Billfree").get(0);
        checkIn(desk, freeVisit);
        assertThat((BigDecimal) itemsOf(freeVisit).get(0).get("amount")).isEqualByComparingTo("0");
    }

    @Test
    void leavingTheQueueCancelsAnUnpaidBillAndCheckingInAgainReopensIt() throws Exception {
        String desk = api.staff("deskbill2", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drbill2").id();
        List<Long> visits = visitsToday(desk, doctorId, "Billqueue");

        checkIn(desk, visits.get(0));
        String number = (String) billOf(visits.get(0)).get("bill_number");
        undoCheckIn(desk, visits.get(0));
        assertThat(statusOf(visits.get(0))).isEqualTo("CANCELLED");
        checkIn(desk, visits.get(0));
        assertThat(statusOf(visits.get(0))).as("reopened").isEqualTo("UNPAID");
        assertThat(billOf(visits.get(0)).get("bill_number")).as("the same bill").isEqualTo(number);

        // Rescheduled after check-in: the unpaid bill is cancelled
        checkIn(desk, visits.get(1));
        long freeSlot = api.get(desk, "/api/receptionist/doctors/" + doctorId + "/next-free-slots?limit=1").get(0)
                .get("slotId").asLong();
        assertThat(api.call(HttpMethod.PUT, "/appointment/update/" + visits.get(1), desk, "{\"slotId\":" + freeSlot + "}")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusOf(visits.get(1))).isEqualTo("CANCELLED");

        // A paid bill stays paid: it covers the visit when it happens
        checkIn(desk, visits.get(2));
        markPaid(visits.get(2));
        undoCheckIn(desk, visits.get(2));
        assertThat(statusOf(visits.get(2))).isEqualTo("PAID");
    }

    @Test
    void aCancelledVisitCancelsAnUnpaidBillAndAPaidOneIsDueForRefund() throws Exception {
        String desk = api.staff("deskbill3", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drbill3").id();
        List<Long> visits = visitsToday(desk, doctorId, "Billcancel");
        checkIn(desk, visits.get(0));
        checkIn(desk, visits.get(1));
        markPaid(visits.get(1));

        cancel(desk, visits.get(0));
        cancel(desk, visits.get(1));

        assertThat(statusOf(visits.get(0))).isEqualTo("CANCELLED");
        assertThat(statusOf(visits.get(1))).isEqualTo("REFUND_DUE");

        // Also when the doctor's approved leave cancels the visit
        checkIn(desk, visits.get(2));
        markPaid(visits.get(2));
        String doctor = api.login("drbill3", "doctor-123");
        ResponseEntity<String> leave = api.call(HttpMethod.POST, "/api/leaves/apply", doctor,
                "{\"startDate\":\"" + LocalDate.now() + "\",\"endDate\":\"" + LocalDate.now() + "\",\"reason\":\"Fever\"}");
        assertThat(leave.getStatusCode()).as(leave.getBody()).isEqualTo(HttpStatus.OK);
        assertThat(api.call(HttpMethod.PUT, "/api/admin/approve/" + json.readTree(leave.getBody()).get("id").asLong(),
                api.admin(), null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusOf(visits.get(2))).isEqualTo("REFUND_DUE");
    }

    // ------------------------------------------------------------------ the front desk's billing API (step B.3)

    private static final String BILLS = "/api/receptionist/bills";

    // A checked-in visit's bill, through the API
    private JsonNode billFor(String token, long appointmentId) throws Exception {
        return api.get(token, BILLS + "/by-appointment/" + appointmentId);
    }

    private JsonNode change(String token, HttpMethod method, String url, String body, HttpStatus expected) throws Exception {
        ResponseEntity<String> res = api.call(method, url, token, body);
        assertThat(res.getStatusCode()).as(url + " " + body + " " + res.getBody()).isEqualTo(expected);
        return json.readTree(res.getBody());
    }

    private static List<String> descriptions(JsonNode bill) {
        List<String> lines = new ArrayList<>();
        // (the JSON says 500.00; read back as a double it is 500.0, hence the scale)
        bill.get("items").forEach(item -> lines.add(item.get("description").asText() + " "
                + item.get("amount").decimalValue().setScale(2, java.math.RoundingMode.HALF_UP)));
        return lines;
    }

    private static BigDecimal amount(JsonNode bill, String field) {
        return bill.get(field).decimalValue();
    }

    @Test
    void theFrontDeskAddsItemsGivesADiscountAndTakesThePayment() throws Exception {
        String desk = api.staff("deskbill4", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drbill4").id();
        setFee(api.admin(), doctorId, "500");
        long visit = visitsToday(desk, doctorId, "Billdesk").get(0);
        assertThat(api.call(HttpMethod.GET, BILLS + "/by-appointment/" + visit, desk, null).getStatusCode())
                .as("no bill before check-in").isEqualTo(HttpStatus.NOT_FOUND);
        checkIn(desk, visit);

        JsonNode bill = billFor(desk, visit);
        long billId = bill.get("id").asLong();
        assertThat(bill.get("status").asText()).isEqualTo("UNPAID");
        assertThat(bill.get("patientName").asText()).isEqualTo("Billdesk 1");
        assertThat(descriptions(bill)).containsExactly("Consultation — Dr. drbill4 500.00");
        assertThat(amount(bill, "total")).isEqualByComparingTo("500");

        // Extra lines and a discount while unpaid
        bill = change(desk, HttpMethod.POST, BILLS + "/" + billId + "/items", "{\"description\":\" Dressing \",\"amount\":150}", HttpStatus.OK);
        bill = change(desk, HttpMethod.POST, BILLS + "/" + billId + "/items", "{\"description\":\"Injection\",\"amount\":99.5}", HttpStatus.OK);
        assertThat(descriptions(bill)).containsExactly("Consultation — Dr. drbill4 500.00", "Dressing 150.00", "Injection 99.50");
        assertThat(amount(bill, "subtotal")).isEqualByComparingTo("749.50");
        long injection = bill.get("items").get(2).get("id").asLong();
        long consultation = bill.get("items").get(0).get("id").asLong();
        bill = change(desk, HttpMethod.DELETE, BILLS + "/" + billId + "/items/" + injection, null, HttpStatus.OK);
        assertThat(amount(bill, "subtotal")).isEqualByComparingTo("650");
        change(desk, HttpMethod.DELETE, BILLS + "/" + billId + "/items/" + consultation, null, HttpStatus.BAD_REQUEST);
        bill = change(desk, HttpMethod.PUT, BILLS + "/" + billId + "/discount", "{\"discount\":50}", HttpStatus.OK);
        assertThat(amount(bill, "total")).isEqualByComparingTo("600");
        for (String wrong : List.of("{\"discount\":650.01}", "{\"discount\":-1}", "{\"discount\":1.234}")) {
            change(desk, HttpMethod.PUT, BILLS + "/" + billId + "/discount", wrong, HttpStatus.BAD_REQUEST);
        }
        for (String wrong : List.of("{\"description\":\"\",\"amount\":10}", "{\"description\":\"Free\",\"amount\":0}",
                "{\"description\":\"x\",\"amount\":100000.01}")) {
            change(desk, HttpMethod.POST, BILLS + "/" + billId + "/items", wrong, HttpStatus.BAD_REQUEST);
        }
        assertThat(api.call(HttpMethod.GET, BILLS + "/" + billId + "/receipt.pdf", desk, null).getStatusCode())
                .as("no receipt before payment").isEqualTo(HttpStatus.CONFLICT);

        // Paid by UPI
        change(desk, HttpMethod.POST, BILLS + "/" + billId + "/pay", "{\"method\":\"bitcoin\"}", HttpStatus.BAD_REQUEST);
        bill = change(desk, HttpMethod.POST, BILLS + "/" + billId + "/pay", "{\"method\":\"upi\",\"reference\":\" UPI-1234 \"}", HttpStatus.OK);
        assertThat(bill.get("status").asText()).isEqualTo("PAID");
        assertThat(bill.get("paymentMethod").asText()).isEqualTo("UPI");
        assertThat(bill.get("paymentReference").asText()).isEqualTo("UPI-1234");
        assertThat(bill.get("paidBy").asText()).isEqualTo("deskbill4");
        assertThat(bill.get("paidAt").isNull()).isFalse();
        assertThat(amount(bill, "total")).isEqualByComparingTo("600");

        // A paid bill doesn't change
        change(desk, HttpMethod.POST, BILLS + "/" + billId + "/items", "{\"description\":\"Late\",\"amount\":10}", HttpStatus.CONFLICT);
        change(desk, HttpMethod.PUT, BILLS + "/" + billId + "/discount", "{\"discount\":0}", HttpStatus.CONFLICT);
        change(desk, HttpMethod.POST, BILLS + "/" + billId + "/pay", "{\"method\":\"CASH\"}", HttpStatus.CONFLICT);
        change(desk, HttpMethod.POST, BILLS + "/" + billId + "/refund", "{\"method\":\"CASH\"}", HttpStatus.CONFLICT);

        // The receipt
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(desk);
        ResponseEntity<byte[]> receipt = rest.exchange(BILLS + "/" + billId + "/receipt.pdf", HttpMethod.GET,
                new HttpEntity<>(headers), byte[].class);
        assertThat(receipt.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(receipt.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(receipt.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("receipt-" + bill.get("billNumber").asText() + ".pdf");
        assertThat(new String(receipt.getBody(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void aPaidVisitThatIsCancelledIsRefundedAtTheDesk() throws Exception {
        String desk = api.staff("deskbill5", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drbill5").id();
        setFee(api.admin(), doctorId, "400");
        List<Long> visits = visitsToday(desk, doctorId, "Billrefund");
        checkIn(desk, visits.get(0));
        long billId = billFor(desk, visits.get(0)).get("id").asLong();
        change(desk, HttpMethod.POST, BILLS + "/" + billId + "/refund", "{\"method\":\"CASH\"}", HttpStatus.CONFLICT);
        change(desk, HttpMethod.POST, BILLS + "/" + billId + "/pay", "{\"method\":\"CARD\"}", HttpStatus.OK);

        cancel(desk, visits.get(0));
        assertThat(api.get(desk, BILLS + "/" + billId).get("status").asText()).isEqualTo("REFUND_DUE");
        JsonNode refunded = change(desk, HttpMethod.POST, BILLS + "/" + billId + "/refund", "{\"method\":\"cash\"}", HttpStatus.OK);

        assertThat(refunded.get("status").asText()).isEqualTo("REFUNDED");
        assertThat(refunded.get("refundMethod").asText()).isEqualTo("CASH");
        assertThat(refunded.get("refundReference").isNull()).isTrue();
        assertThat(refunded.get("refundedBy").asText()).isEqualTo("deskbill5");
        assertThat(refunded.get("paymentMethod").asText()).as("the payment stays on record").isEqualTo("CARD");
        change(desk, HttpMethod.POST, BILLS + "/" + billId + "/refund", "{\"method\":\"CASH\"}", HttpStatus.CONFLICT);
        assertThat(api.call(HttpMethod.GET, BILLS + "/" + billId + "/receipt.pdf", desk, null).getStatusCode())
                .as("the receipt shows the refund").isEqualTo(HttpStatus.OK);
    }

    @Test
    void theBillsListFiltersByDateStatusAndPatientNewestFirst() throws Exception {
        String desk = api.staff("deskbill6", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drbill6").id();
        List<Long> visits = visitsToday(desk, doctorId, "Billlister");
        for (long visit : visits) {
            checkIn(desk, visit);
        }
        long paid = billFor(desk, visits.get(1)).get("id").asLong();
        change(desk, HttpMethod.POST, BILLS + "/" + paid + "/pay", "{\"method\":\"CASH\"}", HttpStatus.OK);
        undoCheckIn(desk, visits.get(2)); // cancelled

        List<String> all = names(api.get(desk, BILLS + "?patient=billlister"));
        assertThat(all).as("newest first").containsExactly("Billlister 3", "Billlister 2", "Billlister 1");
        assertThat(names(api.get(desk, BILLS + "?patient=billlister&status=unpaid"))).containsExactly("Billlister 1");
        assertThat(names(api.get(desk, BILLS + "?patient=billlister&status=PAID"))).containsExactly("Billlister 2");
        assertThat(names(api.get(desk, BILLS + "?patient=billlister&status=cancelled"))).containsExactly("Billlister 3");
        String number = api.get(desk, BILLS + "/" + paid).get("billNumber").asText();
        assertThat(names(api.get(desk, BILLS + "?patient=" + number.toLowerCase()))).as("by bill number").containsExactly("Billlister 2");
        assertThat(names(api.get(desk, BILLS + "?patient=billlister&from=" + LocalDate.now().plusDays(1)))).isEmpty();
        JsonNode page = api.get(desk, BILLS + "?patient=billlister&size=2&page=1");
        assertThat(page.get("totalEntries").asLong()).isEqualTo(3);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        assertThat(names(page)).containsExactly("Billlister 1");
        JsonNode first = api.get(desk, BILLS + "?patient=billlister&size=1").at("/entries/0");
        assertThat(first.get("total").decimalValue()).isEqualByComparingTo("0");
        assertThat(first.get("billNumber").asText()).startsWith("B-");

        assertThat(api.call(HttpMethod.GET, BILLS + "?status=lost", desk, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(api.call(HttpMethod.GET, BILLS + "?size=101", desk, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(api.call(HttpMethod.GET, BILLS + "/987654", desk, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void billsAreForTheFrontDeskAndAdmins() throws Exception {
        String desk = api.staff("deskbill7", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drbill7").id();
        long visit = visitsToday(desk, doctorId, "Billguard").get(0);
        checkIn(desk, visit);
        long billId = billFor(desk, visit).get("id").asLong();

        for (String token : List.of(api.registerPatient("billpeek"), api.login("drbill7", "doctor-123"))) {
            assertThat(api.call(HttpMethod.GET, BILLS, token, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(api.call(HttpMethod.GET, BILLS + "/" + billId, token, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(api.call(HttpMethod.POST, BILLS + "/" + billId + "/pay", token, "{\"method\":\"CASH\"}")
                    .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(billFor(api.admin(), visit).get("id").asLong()).isEqualTo(billId);
    }

    // ------------------------------------------------------------------ the patient's own bills (PATIENT_BILLS_PLAN.md)

    private static final String MY_BILLS = "/api/bills/my";

    private long bookedForToday(String patient, long slotId, Long relativeId) throws Exception {
        return api.bookedForToday(patient, slotId, relativeId, jdbc);
    }

    private JsonNode freeSlots(String token, long doctorId, String shift) throws Exception {
        return api.freeSlotsTomorrow(token, doctorId, shift);
    }

    private static List<Long> ids(JsonNode bills) {
        List<Long> ids = new ArrayList<>();
        bills.forEach(b -> ids.add(b.get("id").asLong()));
        return ids;
    }

    private ResponseEntity<byte[]> pdf(String token, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    @Test
    void patientsSeeTheirOwnAndTheirFamilysBillsAndDownloadTheReceipts() throws Exception {
        String desk = api.staff("deskbill8", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drbill8").id();
        setFee(api.admin(), doctorId, "450");
        String patient = api.registerPatient("billmine");
        String other = api.registerPatient("billother");
        long sonId = json.readTree(api.call(HttpMethod.POST, "/api/patient/relative/add", patient,
                "{\"name\":\"Little Billy\",\"gender\":\"MALE\",\"dob\":\"2018-05-01\",\"relationship\":\"SON\"}")
                .getBody()).get("id").asLong();
        JsonNode evening = freeSlots(patient, doctorId, "EVENING");
        long own = bookedForToday(patient, evening.get(0).get("id").asLong(), null);
        long sons = bookedForToday(patient, evening.get(1).get("id").asLong(), sonId);
        long others = bookedForToday(other, freeSlots(other, doctorId, "MORNING").get(0).get("id").asLong(), null);
        assertThat(api.get(patient, MY_BILLS)).as("no bill before check-in").isEmpty();

        for (long visit : List.of(own, sons, others)) {
            checkIn(desk, visit);
        }
        long ownBill = billFor(desk, own).get("id").asLong();
        long sonsBill = billFor(desk, sons).get("id").asLong();
        long othersBill = billFor(desk, others).get("id").asLong();
        change(desk, HttpMethod.POST, BILLS + "/" + ownBill + "/pay", "{\"method\":\"UPI\",\"reference\":\"UPI-77\"}", HttpStatus.OK);

        JsonNode mine = api.get(patient, MY_BILLS);
        assertThat(ids(mine)).as("their own and their son's, newest first").containsExactly(sonsBill, ownBill);
        JsonNode sonsUnpaid = mine.get(0);
        assertThat(sonsUnpaid.get("patientName").asText()).isEqualTo("Little Billy");
        assertThat(sonsUnpaid.get("appointmentId").asLong()).isEqualTo(sons);
        assertThat(sonsUnpaid.get("status").asText()).isEqualTo("UNPAID");
        assertThat(sonsUnpaid.get("payOnline").asBoolean()).as("no Razorpay keys here").isFalse();
        assertThat(api.call(HttpMethod.POST, MY_BILLS + "/" + sonsBill + "/online-payment", patient, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        JsonNode paid = mine.get(1);
        assertThat(paid.get("appointmentId").asLong()).isEqualTo(own);
        assertThat(paid.get("billNumber").asText()).startsWith("B-");
        assertThat(paid.get("doctorName").asText()).isEqualTo("drbill8");
        assertThat(paid.get("visitDate").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(descriptions(paid)).containsExactly("Consultation — Dr. drbill8 450.00");
        assertThat(amount(paid, "total")).isEqualByComparingTo("450");
        assertThat(amount(paid, "discount")).isEqualByComparingTo("0");
        assertThat(paid.get("status").asText()).isEqualTo("PAID");
        assertThat(paid.get("paymentMethod").asText()).isEqualTo("UPI");
        assertThat(paid.get("paymentReference").asText()).isEqualTo("UPI-77");
        assertThat(paid.get("paidAt").isNull()).isFalse();
        assertThat(paid.has("paidBy")).as("who took the money is the desk's business").isFalse();
        assertThat(paid.has("patientId")).isFalse();
        assertThat(paid.at("/items/0").has("id")).isFalse();

        // The receipt once paid; another patient's bill is not found, paid or not
        ResponseEntity<byte[]> receipt = pdf(patient, MY_BILLS + "/" + ownBill + "/receipt.pdf");
        assertThat(receipt.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(receipt.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(receipt.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("receipt-" + paid.get("billNumber").asText() + ".pdf");
        assertThat(new String(receipt.getBody(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(api.call(HttpMethod.GET, MY_BILLS + "/" + sonsBill + "/receipt.pdf", patient, null).getStatusCode())
                .as("not paid yet").isEqualTo(HttpStatus.CONFLICT);
        change(desk, HttpMethod.POST, BILLS + "/" + othersBill + "/pay", "{\"method\":\"CASH\"}", HttpStatus.OK);
        assertThat(api.call(HttpMethod.GET, MY_BILLS + "/" + othersBill + "/receipt.pdf", patient, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ids(api.get(other, MY_BILLS))).containsExactly(othersBill);
        assertThat(pdf(other, MY_BILLS + "/" + othersBill + "/receipt.pdf").getStatusCode()).isEqualTo(HttpStatus.OK);

        // A cancelled bill was never paid: the son left the queue, so it is left out
        undoCheckIn(desk, sons);
        assertThat(ids(api.get(patient, MY_BILLS))).containsExactly(ownBill);
        assertThat(api.call(HttpMethod.GET, MY_BILLS + "/" + sonsBill + "/receipt.pdf", patient, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void myBillsAreForPatientsOnly() throws Exception {
        for (String token : List.of(api.staff("deskbill9", "RECEPTIONIST"), api.staff("drbill9", "DOCTOR"), api.admin())) {
            assertThat(api.call(HttpMethod.GET, MY_BILLS, token, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(api.call(HttpMethod.GET, MY_BILLS + "/1/receipt.pdf", token, null).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(api.call(HttpMethod.GET, MY_BILLS, null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static List<String> names(JsonNode page) {
        List<String> names = new ArrayList<>();
        page.get("entries").forEach(b -> names.add(b.get("patientName").asText()));
        return names;
    }
}
