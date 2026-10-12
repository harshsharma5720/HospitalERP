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
 * Today's queue (docs/TODAYS_QUEUE_PLAN.md): check-in and tokens (step Q.1), the doctor's queue and the waiting-room
 * screen (step Q.2). Same configuration as FeatureFlowH2Test,
 * so both share one application context; the phone numbers 9000044xxx are used by no other test. Bookings are
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
    private int batch = 0;
    private int nextPhone = 0;

    @BeforeEach
    void setUpApi() {
        api = new TestApi(rest, json);
    }

    // Three visits today (10:00, 16:00, 16:30), see TestApi.visitsToday; phones 9000044b0n, one batch b per call
    private List<Long> visitsToday(String desk, long doctorId, String surname) throws Exception {
        return api.visitsToday(desk, doctorId, surname, "9000044" + (++batch), jdbc);
    }

    // Other numbers: 900004490n
    private String phone() {
        return "90000449%02d".formatted(++nextPhone);
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

    // ------------------------------------------------------------------ the doctor's queue and the screen (step Q.2)

    private JsonNode queue(String doctor) throws Exception {
        return api.get(doctor, "/api/doctor/queue");
    }

    private static List<Long> ids(JsonNode appointments) {
        List<Long> ids = new ArrayList<>();
        appointments.forEach(a -> ids.add(a.get("appointmentID").asLong()));
        return ids;
    }

    private ResponseEntity<String> doctorAction(String doctor, String path) {
        return api.call(HttpMethod.POST, "/api/doctor/queue" + path, doctor, null);
    }

    @Test
    void theDoctorCallsTheirPatientsInTheOrderOfTheirTimes() throws Exception {
        String desk = api.staff("deskqueue5", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drqueue5").id();
        String doctor = api.login("drqueue5", "doctor-123");
        List<Long> visits = visitsToday(desk, doctorId, "Queuecall"); // 10:00, 16:00, 16:30
        checkedIn(desk, visits.get(2));
        checkedIn(desk, visits.get(0));

        JsonNode today = queue(doctor);
        assertThat(today.get("date").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(ids(today.get("waiting"))).as("by time, not arrival").containsExactly(visits.get(0), visits.get(2));
        assertThat(ids(today.get("notArrived"))).containsExactly(visits.get(1));
        assertThat(today.get("withDoctor")).isEmpty();
        assertThat(today.get("done")).isEmpty();

        // Call next: the 10:00 patient
        ResponseEntity<String> called = doctorAction(doctor, "/next");
        assertThat(called.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(called.getBody()).get("appointmentID").asLong()).isEqualTo(visits.get(0));
        assertThat(json.readTree(called.getBody()).get("calledAt").isNull()).isFalse();
        assertThat(ids(queue(doctor).get("withDoctor"))).containsExactly(visits.get(0));
        assertThat(ids(queue(doctor).get("waiting"))).containsExactly(visits.get(2));

        // The visit is completed as before; then the next one
        assertThat(api.call(HttpMethod.PUT, "/api/doctor/complete/" + visits.get(0), doctor, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(ids(queue(doctor).get("done"))).containsExactly(visits.get(0));
        assertThat(queue(doctor).get("withDoctor")).isEmpty();
        assertThat(json.readTree(doctorAction(doctor, "/next").getBody()).get("appointmentID").asLong())
                .isEqualTo(visits.get(2));

        // Nobody else has arrived
        ResponseEntity<String> nobody = doctorAction(doctor, "/next");
        assertThat(nobody.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(nobody.getBody()).contains("Nobody is waiting");

        // Not here: back to waiting, with the same token; then called by name
        JsonNode back = json.readTree(doctorAction(doctor, "/" + visits.get(2) + "/not-here").getBody());
        assertThat(back.get("calledAt").isNull()).isTrue();
        assertThat(back.get("tokenNumber").asInt()).isEqualTo(3);
        assertThat(ids(queue(doctor).get("waiting"))).containsExactly(visits.get(2));
        assertThat(doctorAction(doctor, "/" + visits.get(2) + "/call").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(queue(doctor).get("withDoctor"))).containsExactly(visits.get(2));
    }

    @Test
    void onlyDoctorsAndOnlyTheirOwnCheckedInPatients() throws Exception {
        String desk = api.staff("deskqueue6", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drqueue6").id();
        api.doctorWithSchedule("drqueue6b");
        String doctor = api.login("drqueue6", "doctor-123");
        String otherDoctor = api.login("drqueue6b", "doctor-123");
        List<Long> visits = visitsToday(desk, doctorId, "Queueown");
        checkedIn(desk, visits.get(0));

        assertThat(doctorAction(otherDoctor, "/" + visits.get(0) + "/call").getStatusCode()).as("someone else's patient")
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(queue(otherDoctor).get("waiting")).as("an empty queue of their own").isEmpty();
        assertThat(doctorAction(doctor, "/" + visits.get(1) + "/call").getStatusCode()).as("not arrived")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(doctorAction(doctor, "/" + visits.get(0) + "/not-here").getStatusCode()).as("not called")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(doctorAction(doctor, "/987654/call").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // The queue is the doctor's own: not for the front desk, not for admins (who have no queue)
        for (String token : List.of(desk, api.admin(), api.registerPatient("queuepeek2"))) {
            assertThat(api.call(HttpMethod.GET, "/api/doctor/queue", token, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(doctorAction(token, "/next").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void theWaitingRoomScreenShowsTokensOnlyAndNeedsNoLogin() throws Exception {
        String desk = api.staff("deskqueue7", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drqueue7").id();
        api.doctorWithSchedule("drqueue7b"); // nobody checked in: not on the screen
        String doctor = api.login("drqueue7", "doctor-123");
        List<Long> visits = visitsToday(desk, doctorId, "Queuescreen");
        checkedIn(desk, visits.get(0));
        checkedIn(desk, visits.get(1));
        checkedIn(desk, visits.get(2));
        doctorAction(doctor, "/next"); // token 1
        doctorAction(doctor, "/next"); // token 2: now being served

        ResponseEntity<String> screen = api.call(HttpMethod.GET, "/api/queue/screen", null, null);

        assertThat(screen.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(screen.getBody()).as("no patient names").doesNotContain("Queuescreen");
        JsonNode body = json.readTree(screen.getBody());
        assertThat(body.get("date").asText()).isEqualTo(LocalDate.now().toString());
        JsonNode mine = null;
        List<String> names = new ArrayList<>();
        for (JsonNode d : body.get("doctors")) {
            names.add(d.get("doctorName").asText());
            if ("drqueue7".equals(d.get("doctorName").asText())) {
                mine = d;
            }
        }
        assertThat(names).doesNotContain("drqueue7b");
        assertThat(mine).as("the doctor's user name while the profile has no name").isNotNull();
        assertThat(mine.get("nowServing").asInt()).as("the last one called in").isEqualTo(2);
        assertThat(mine.get("next")).extracting(JsonNode::asInt).containsExactly(3);
        assertThat(mine.fieldNames()).toIterable().containsExactlyInAnyOrder("doctorName", "specialist", "nowServing", "next");
    }
}
