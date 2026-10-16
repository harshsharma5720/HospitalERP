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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A doctor's earnings (docs/DOCTOR_ANALYTICS_PLAN.md) from bills made at the desk. Same configuration as
 * FeatureFlowH2Test, so both share one application context; the phone numbers 9000067xxx are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class DoctorEarningsH2Test {

    private static final String BILLS = "/api/receptionist/bills";

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    private long doctorWithFee(String username) throws Exception {
        long doctorId = api.doctorWithSchedule(username).id();
        assertThat(api.call(HttpMethod.PUT, "/api/admin/fees/" + doctorId, api.admin(), "{\"consultationFee\":500}")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        return doctorId;
    }

    private void ok(String token, HttpMethod method, String url, String body) {
        assertThat(api.call(method, url, token, body).getStatusCode()).as(url).isEqualTo(HttpStatus.OK);
    }

    // Checks the visit in at the desk; its bill's id
    private long checkedIn(String desk, long appointmentId) throws Exception {
        ok(desk, HttpMethod.POST, "/api/receptionist/appointments/" + appointmentId + "/check-in", null);
        return api.get(desk, BILLS + "/by-appointment/" + appointmentId).get("id").asLong();
    }

    @Test
    void aDoctorsEarningsPaidRefundedAndUnpaid() throws Exception {
        String desk = api.staff("deskearn", "RECEPTIONIST");
        long doctorId = doctorWithFee("drearn");
        LocalDate today = LocalDate.now();
        List<Long> visits = api.visitsToday(desk, doctorId, "Earner", "900006701", jdbc);
        long paid = checkedIn(desk, visits.get(0));
        long refundDue = checkedIn(desk, visits.get(1));
        checkedIn(desk, visits.get(2)); // stays unpaid

        ok(desk, HttpMethod.POST, BILLS + "/" + paid + "/items", "{\"description\":\"Dressing\",\"amount\":100}");
        ok(desk, HttpMethod.PUT, BILLS + "/" + paid + "/discount", "{\"discount\":50}");
        ok(desk, HttpMethod.POST, BILLS + "/" + paid + "/pay", "{\"method\":\"CASH\"}"); // 550
        jdbc.update("UPDATE bills SET visit_date = ? WHERE id = ?", today.minusDays(1), paid);
        ok(desk, HttpMethod.POST, BILLS + "/" + refundDue + "/pay", "{\"method\":\"CARD\"}");
        ok(desk, HttpMethod.DELETE, "/api/receptionist/deleteAppointment/" + visits.get(1), null); // refund due 500

        // A cancelled bill (checked in, then the check-in undone) brought in nothing
        String patient = api.registerPatient("earnpatient");
        long slotId = api.get(patient, "/api/slots/available/" + doctorId + "?date=" + today.plusDays(2)
                + "&shift=MORNING").get(0).get("id").asLong();
        long fourth = api.bookedForToday(patient, slotId, null, jdbc);
        checkedIn(desk, fourth);
        ok(desk, HttpMethod.DELETE, "/api/receptionist/appointments/" + fourth + "/check-in", null);

        // Another doctor's paid bill doesn't count
        long other = doctorWithFee("drearn2");
        long otherBill = checkedIn(desk, api.visitsToday(desk, other, "Earnother", "900006702", jdbc).get(0));
        ok(desk, HttpMethod.POST, BILLS + "/" + otherBill + "/pay", "{\"method\":\"UPI\"}");

        String token = api.login("drearn", "doctor-123");
        JsonNode earnings = api.get(token, "/api/doctor/earnings?days=7");
        assertThat(earnings.at("/period/days").asInt()).isEqualTo(7);
        assertThat(earnings.at("/period/from").asText()).isEqualTo(today.minusDays(6).toString());
        assertThat(earnings.get("earned").decimalValue()).isEqualByComparingTo("550");
        assertThat(earnings.get("paidVisits").asLong()).isEqualTo(1);
        assertThat(earnings.get("averagePerVisit").decimalValue()).isEqualByComparingTo("550");
        assertThat(earnings.get("refunds").decimalValue()).isEqualByComparingTo("500");
        assertThat(earnings.get("refundedVisits").asLong()).isEqualTo(1);
        assertThat(earnings.get("unpaid").decimalValue()).isEqualByComparingTo("500");
        assertThat(earnings.get("unpaidVisits").asLong()).isEqualTo(1);
        JsonNode perDay = earnings.get("perDay");
        assertThat(perDay).hasSize(7);
        assertThat(perDay.get(5).get("date").asText()).isEqualTo(today.minusDays(1).toString());
        assertThat(perDay.get(5).get("earned").decimalValue()).as("yesterday").isEqualByComparingTo("550");
        assertThat(perDay.get(6).get("earned").decimalValue()).as("today: nothing kept").isEqualByComparingTo("0");

        JsonNode seenByAdmin = api.get(api.admin(), "/api/admin/doctors/" + doctorId + "/earnings?days=7");
        assertThat(seenByAdmin).isEqualTo(earnings);
        assertThat(api.get(token, "/api/doctor/earnings").at("/period/days").asInt()).as("30 by default").isEqualTo(30);
    }

    @Test
    void noEarningsYetPeriodsAndAccess() throws Exception {
        long doctorId = doctorWithFee("drearn3");
        String token = api.login("drearn3", "doctor-123");
        JsonNode none = api.get(token, "/api/doctor/earnings?days=90");
        assertThat(none.get("earned").decimalValue()).isEqualByComparingTo("0");
        assertThat(none.get("averagePerVisit").isNull()).as("no paid visit").isTrue();
        assertThat(none.get("perDay")).hasSize(90);

        assertThat(api.call(HttpMethod.GET, "/api/doctor/earnings?days=14", token, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(api.call(HttpMethod.GET, "/api/admin/doctors/" + doctorId + "/earnings", token, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        for (String other : List.of(api.staff("deskearn3", "RECEPTIONIST"), api.registerPatient("earnpeek"))) {
            assertThat(api.call(HttpMethod.GET, "/api/doctor/earnings", other, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(api.call(HttpMethod.GET, "/api/doctor/earnings", api.admin(), null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.call(HttpMethod.GET, "/api/admin/doctors/987654/earnings", api.admin(), null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
