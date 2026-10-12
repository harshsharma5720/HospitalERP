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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Billing (docs/BILLING_PLAN.md): consultation fees per doctor (step B.1), bills made and changed by the queue
 * (step B.2). Same configuration as FeatureFlowH2Test, so both share one application context; the phone numbers
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
}
