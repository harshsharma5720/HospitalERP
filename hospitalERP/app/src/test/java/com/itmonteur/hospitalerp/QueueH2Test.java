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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Today's queue (docs/TODAYS_QUEUE_PLAN.md): check-in and tokens (step Q.1). Same configuration as FeatureFlowH2Test,
 * so both share one application context; the phone numbers +91 90000 444xx are used by no other test. Bookings are
 * made for tomorrow and then moved to today in the database, so the tests don't depend on the time of day.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class QueueH2Test {

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;
    private int nextPhone = 0;

    @BeforeEach
    void setUpApi() {
        api = new TestApi(rest, json);
    }

    /**
     * Three walk-in visits with a new doctor - booked for tomorrow 10:00, 16:00 and 16:30, then moved to today.
     * Returns their appointment ids in time order.
     */
    private List<Long> visitsToday(String desk, long doctorId, String surname) throws Exception {
        JsonNode slots = api.get(desk, "/api/receptionist/doctors/" + doctorId + "/next-free-slots?limit=3");
        List<Long> ids = new ArrayList<>();
        for (JsonNode slot : slots) {
            ResponseEntity<String> res = api.call(HttpMethod.POST, "/api/receptionist/walk-in", desk,
                    "{\"newPatient\":{\"name\":\"" + surname + " " + (ids.size() + 1) + "\",\"phone\":\"%s\",\"gender\":\"OTHER\"},"
                            .formatted(phone()) + "\"age\":40,\"slotId\":" + slot.get("slotId").asLong() + "}");
            assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(HttpStatus.OK);
            JsonNode appointment = json.readTree(res.getBody()).get("appointment");
            assertThat(appointment.get("tokenNumber").isNull()).as("booked for tomorrow: not checked in").isTrue();
            ids.add(appointment.get("appointmentID").asLong());
            jdbc.update("UPDATE slots SET date = ? WHERE id = ?", LocalDate.now(), slot.get("slotId").asLong());
            jdbc.update("UPDATE appointments SET date = ? WHERE appointmentid = ?", LocalDate.now(), ids.get(ids.size() - 1));
        }
        return ids;
    }

    private String phone() {
        return "90000444%02d".formatted(++nextPhone);
    }

    private ResponseEntity<String> checkIn(String token, long appointmentId) {
        return api.call(HttpMethod.POST, "/api/receptionist/appointments/" + appointmentId + "/check-in", token, null);
    }

    private JsonNode checkedIn(String token, long appointmentId) throws Exception {
        ResponseEntity<String> res = checkIn(token, appointmentId);
        assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody());
    }

    @Test
    void tokensFollowTheBookedTimeNotTheArrival() throws Exception {
        String desk = api.staff("deskqueue", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drqueue").id();
        List<Long> visits = visitsToday(desk, doctorId, "Queueton"); // 10:00, 16:00, 16:30

        // The 16:30 patient arrives first, but keeps the place of 16:30 in the doctor's day
        JsonNode last = checkedIn(desk, visits.get(2));
        assertThat(last.get("tokenNumber").asInt()).isEqualTo(3);
        LocalDateTime arrived = LocalDateTime.parse(last.get("checkedInAt").asText());
        assertThat(arrived).isBetween(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusSeconds(1));
        assertThat(checkedIn(desk, visits.get(0)).get("tokenNumber").asInt()).isEqualTo(1);

        // Twice: the first token and time are kept
        JsonNode again = checkedIn(desk, visits.get(2));
        assertThat(again.get("tokenNumber").asInt()).isEqualTo(3);
        assertThat(again.get("checkedInAt").asText()).isEqualTo(last.get("checkedInAt").asText());

        // The front-desk list shows who has arrived
        JsonNode today = api.get(desk, "/api/receptionist/appointments?doctorId=" + doctorId).get("entries");
        assertThat(today).hasSize(3);
        assertThat(today.get(0).get("tokenNumber").asInt()).isEqualTo(1);
        assertThat(today.get(1).get("tokenNumber").isNull()).as("not arrived").isTrue();
        assertThat(today.get(1).get("checkedInAt").isNull()).isTrue();
        assertThat(today.get(2).get("tokenNumber").asInt()).isEqualTo(3);

        // A mistake can be undone; checking in again gives the same token
        ResponseEntity<String> undone = api.call(HttpMethod.DELETE,
                "/api/receptionist/appointments/" + visits.get(0) + "/check-in", desk, null);
        assertThat(undone.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(undone.getBody()).get("tokenNumber").isNull()).isTrue();
        assertThat(checkedIn(desk, visits.get(0)).get("tokenNumber").asInt()).isEqualTo(1);

        // Admins can do it too
        assertThat(checkedIn(api.admin(), visits.get(1)).get("tokenNumber").asInt()).isEqualTo(2);
    }

    @Test
    void aTokenAlreadyGivenOutThatDayIsNotGivenTwice() throws Exception {
        String desk = api.staff("deskqueue2", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drqueue2").id();
        List<Long> visits = visitsToday(desk, doctorId, "Queueman");
        checkedIn(desk, visits.get(0));
        // As if a schedule change during the day had renumbered the slots: 3 is already someone's token
        jdbc.update("UPDATE appointments SET token_number = 3 WHERE appointmentid = ?", visits.get(0));

        assertThat(checkedIn(desk, visits.get(2)).get("tokenNumber").asInt()).as("the next free number").isEqualTo(4);
    }

    @Test
    void onlyTodaysBookedAppointmentsAndNotOnceCalledIn() throws Exception {
        String desk = api.staff("deskqueue3", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drqueue3").id();
        JsonNode laterSlot = api.get(desk, "/api/receptionist/doctors/" + doctorId + "/next-free-slots?limit=4").get(3);
        List<Long> visits = visitsToday(desk, doctorId, "Queuewell");
        ResponseEntity<String> later = api.call(HttpMethod.POST, "/api/receptionist/walk-in", desk,
                "{\"newPatient\":{\"name\":\"Queuewell Later\",\"phone\":\"%s\",\"gender\":\"OTHER\"},".formatted(phone())
                        + "\"age\":40,\"slotId\":" + laterSlot.get("slotId").asLong() + "}");
        long laterVisit = json.readTree(later.getBody()).at("/appointment/appointmentID").asLong(); // the day after tomorrow

        assertThat(checkIn(desk, laterVisit).getStatusCode()).as("not today").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(checkIn(desk, laterVisit).getBody()).contains("today");
        assertThat(api.call(HttpMethod.DELETE, "/api/receptionist/deleteAppointment/" + visits.get(1), desk, null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(checkIn(desk, visits.get(1)).getStatusCode()).as("cancelled").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(checkIn(desk, 987654).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Once the doctor has called the patient in, the check-in stays
        checkedIn(desk, visits.get(0));
        jdbc.update("UPDATE appointments SET called_at = ? WHERE appointmentid = ?", LocalDateTime.now(), visits.get(0));
        assertThat(api.call(HttpMethod.DELETE, "/api/receptionist/appointments/" + visits.get(0) + "/check-in", desk, null)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Patients and doctors can't check anyone in
        assertThat(checkIn(api.registerPatient("queuepeek"), visits.get(2)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(checkIn(api.login("drqueue3", "doctor-123"), visits.get(2)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void reschedulingClearsTheQueueState() throws Exception {
        String desk = api.staff("deskqueue4", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drqueue4").id();
        List<Long> visits = visitsToday(desk, doctorId, "Queueshift");
        checkedIn(desk, visits.get(0));
        long freeSlot = api.get(desk, "/api/receptionist/doctors/" + doctorId + "/next-free-slots?limit=1").get(0)
                .get("slotId").asLong(); // a free slot on another day

        ResponseEntity<String> moved = api.call(HttpMethod.PUT, "/appointment/update/" + visits.get(0), desk,
                "{\"slotId\":" + freeSlot + "}");

        assertThat(moved.getStatusCode()).as(moved.getBody()).isEqualTo(HttpStatus.OK);
        JsonNode appointment = json.readTree(moved.getBody());
        assertThat(appointment.get("tokenNumber").isNull()).isTrue();
        assertThat(appointment.get("checkedInAt").isNull()).isTrue();
    }
}
