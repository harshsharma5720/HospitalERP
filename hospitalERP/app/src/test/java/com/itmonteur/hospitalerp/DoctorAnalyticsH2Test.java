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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A doctor's analytics (docs/DOCTOR_ANALYTICS_PLAN.md) from visits set up in the database. Same configuration as
 * FeatureFlowH2Test, so both share one application context; the phone numbers 9000066xxx are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class DoctorAnalyticsH2Test {

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    // Moves a visit (and its slot) to that day, with that status and queue times
    private void set(long appointmentId, LocalDate day, String status, LocalDateTime checkedIn, LocalDateTime called) {
        jdbc.update("UPDATE slots SET date = ? WHERE id = (SELECT slot_id FROM appointments WHERE appointmentid = ?)",
                day, appointmentId);
        jdbc.update("UPDATE appointments SET date = ?, status = ?, checked_in_at = ?, called_at = ? WHERE appointmentid = ?",
                day, status, checkedIn, called, appointmentId);
    }

    private static List<Long> longs(JsonNode array, String field) {
        List<Long> values = new ArrayList<>();
        array.forEach(n -> values.add(n.get(field).asLong()));
        return values;
    }

    @Test
    void aDoctorsVisitsPerDayRatesBusyHoursAndWaitingTime() throws Exception {
        String desk = api.staff("deskanalytics", "RECEPTIONIST");
        TestApi.Doctor doctor = api.doctorWithSchedule("dranalytics");
        LocalDate today = LocalDate.now();
        // Slots 10:00, 16:00, 16:30 today, then moved
        List<Long> visits = api.visitsToday(desk, doctor.id(), "Analytic", "900006601", jdbc);
        long completed = visits.get(0), missed = visits.get(1), upcoming = visits.get(2);
        set(completed, today.minusDays(1), "COMPLETED",
                today.minusDays(1).atTime(9, 50), today.minusDays(1).atTime(10, 5)); // waited 15 minutes
        set(missed, today.minusDays(2), "SCHEDULED", null, null);
        set(upcoming, today, "CONFIRMED", today.atTime(15, 50), today.atTime(16, 20)); // waited 30 minutes
        // A cancelled visit three days ago (the day-after-tomorrow 09:00 slot)
        String patient = api.registerPatient("analyticspatient");
        long slotId = api.get(patient, "/api/slots/available/" + doctor.id() + "?date=" + today.plusDays(2)
                + "&shift=MORNING").get(0).get("id").asLong();
        long cancelled = api.bookedForToday(patient, slotId, null, jdbc);
        set(cancelled, today.minusDays(3), "CANCELLED_BY_PATIENT", null, null);
        // Another doctor's visits don't count
        TestApi.Doctor other = api.doctorWithSchedule("dranalytics2");
        api.visitsToday(desk, other.id(), "Analyticother", "900006602", jdbc);

        String token = api.login("dranalytics", "doctor-123");
        JsonNode figures = api.get(token, "/api/doctor/analytics?days=7");

        assertThat(figures.at("/period/days").asInt()).isEqualTo(7);
        assertThat(figures.at("/period/from").asText()).isEqualTo(today.minusDays(6).toString());
        assertThat(figures.at("/period/to").asText()).isEqualTo(today.toString());
        assertThat(figures.at("/doctor/id").asLong()).isEqualTo(doctor.id());
        assertThat(figures.at("/doctor/name").asText()).as("the user name while the profile has none").isEqualTo("dranalytics");

        JsonNode totals = figures.get("visits");
        assertThat(totals.get("total").asLong()).isEqualTo(4);
        assertThat(totals.get("completed").asLong()).isEqualTo(1);
        assertThat(totals.get("missed").asLong()).isEqualTo(1);
        assertThat(totals.get("upcoming").asLong()).isEqualTo(1);
        assertThat(totals.get("cancelled").asLong()).isEqualTo(1);
        assertThat(totals.get("cancellationRate").asDouble()).as("1 of 4").isEqualTo(25.0);
        assertThat(totals.get("noShowRate").asDouble()).as("1 missed of 2 past, not cancelled").isEqualTo(50.0);

        JsonNode perDay = figures.get("perDay");
        assertThat(perDay).hasSize(7);
        assertThat(perDay.get(0).get("date").asText()).isEqualTo(today.minusDays(6).toString());
        assertThat(perDay.get(3).get("cancelled").asLong()).as("3 days ago").isEqualTo(1);
        assertThat(perDay.get(4).get("missed").asLong()).as("2 days ago").isEqualTo(1);
        assertThat(perDay.get(5).get("completed").asLong()).as("yesterday").isEqualTo(1);
        assertThat(perDay.get(6).get("upcoming").asLong()).as("today").isEqualTo(1);
        assertThat(perDay.get(6).get("missed").asLong()).as("today's open visit isn't missed yet").isZero();

        // Busy hours: 10:00 and 16:00 / 16:30, the cancelled 09:00 left out; every hour from 10 to 16
        JsonNode byHour = figures.get("byHour");
        assertThat(longs(byHour, "hour")).containsExactly(10L, 11L, 12L, 13L, 14L, 15L, 16L);
        assertThat(longs(byHour, "visits")).containsExactly(1L, 0L, 0L, 0L, 0L, 0L, 2L);
        JsonNode byWeekday = figures.get("byWeekday");
        assertThat(byWeekday).hasSize(7);
        assertThat(byWeekday.get(0).get("day").asText()).isEqualTo("MONDAY");
        long[] expected = new long[7];
        for (LocalDate day : List.of(today.minusDays(1), today.minusDays(2), today)) {
            expected[day.getDayOfWeek().ordinal()]++;
        }
        for (DayOfWeek day : DayOfWeek.values()) {
            assertThat(byWeekday.get(day.ordinal()).get("visits").asLong()).as(day.toString()).isEqualTo(expected[day.ordinal()]);
        }

        JsonNode waiting = figures.get("waiting");
        assertThat(waiting.get("measured").asLong()).isEqualTo(2);
        assertThat(waiting.get("averageMinutes").asDouble()).isEqualTo(22.5);
        assertThat(waiting.get("longestMinutes").asLong()).isEqualTo(30);
        assertThat(waiting.get("perDay")).hasSize(7);
        assertThat(waiting.at("/perDay/5/averageMinutes").asDouble()).isEqualTo(15.0);
        assertThat(waiting.at("/perDay/6/averageMinutes").asDouble()).isEqualTo(30.0);
        assertThat(waiting.at("/perDay/0/averageMinutes").isNull()).as("nothing measured").isTrue();
        assertThat(waiting.at("/perDay/0/measured").asLong()).isZero();

        // The admin sees the same for that doctor; 30 days has 30 days
        assertThat(api.get(api.admin(), "/api/admin/doctors/" + doctor.id() + "/analytics?days=7").get("visits")).isEqualTo(totals);
        assertThat(api.get(token, "/api/doctor/analytics?days=30").get("perDay")).hasSize(30);
        assertThat(api.get(token, "/api/doctor/analytics").at("/period/days").asInt()).as("30 by default").isEqualTo(30);
    }

    @Test
    void periodsAndAccess() throws Exception {
        TestApi.Doctor doctor = api.doctorWithSchedule("dranalytics3");
        String token = api.login("dranalytics3", "doctor-123");
        JsonNode empty = api.get(token, "/api/doctor/analytics?days=90");
        assertThat(empty.at("/visits/total").asLong()).isZero();
        assertThat(empty.at("/visits/noShowRate").asDouble()).isZero();
        assertThat(empty.get("byHour")).isEmpty();
        assertThat(empty.at("/waiting/averageMinutes").isNull()).isTrue();
        assertThat(empty.at("/waiting/longestMinutes").isNull()).isTrue();

        assertThat(api.call(HttpMethod.GET, "/api/doctor/analytics?days=8", token, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(api.call(HttpMethod.GET, "/api/admin/doctors/" + doctor.id() + "/analytics", token, null).getStatusCode())
                .as("another doctor's, or even their own, through the admin path").isEqualTo(HttpStatus.FORBIDDEN);
        for (String other : List.of(api.staff("deskanalytics3", "RECEPTIONIST"), api.registerPatient("analyticspeek"))) {
            assertThat(api.call(HttpMethod.GET, "/api/doctor/analytics", other, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
        ResponseEntity<String> adminOwn = api.call(HttpMethod.GET, "/api/doctor/analytics", api.admin(), null);
        assertThat(adminOwn.getStatusCode()).as("an admin has no doctor profile").isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.call(HttpMethod.GET, "/api/admin/doctors/987654/analytics", api.admin(), null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
