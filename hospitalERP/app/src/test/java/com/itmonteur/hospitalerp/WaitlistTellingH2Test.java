package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmonteur.hospitalerp.waitlist.internal.WaitlistCleanup;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Telling the people waiting (docs/WAITLIST_PLAN.md, WL.2): a freed time to everyone waiting for that doctor, day and
 * shift, by the bell and SMS, at most once in 30 minutes; the doctor's deactivation or leave ends the waits. Same
 * configuration as FeatureFlowH2Test, so both share one application context; the accounts *wlt* and the phones
 * 9000081xxx are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class WaitlistTellingH2Test {

    private static final String MINE = "/api/waitlist";
    private static final String DESK = "/api/receptionist/waitlist";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private WaitlistCleanup cleanup;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    private long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    // The waitlist's bell messages of a user, oldest first: "KIND title | body -> link"
    private List<String> bell(String username) {
        return jdbc.queryForList("SELECT CONCAT(kind, ' ', title, ' | ', body, ' -> ', link) FROM inbox_messages"
                + " WHERE user_id = ? AND kind LIKE 'WAITLIST%' ORDER BY id", String.class, userId(username));
    }

    // The waitlist's SMS mentioning a doctor: "recipient: text"
    private List<String> sms(String doctor) {
        return jdbc.queryForList("SELECT CONCAT(recipient, ': ', body) FROM notification_outbox WHERE channel = 'SMS'"
                + " AND description LIKE 'waitlist%' AND body LIKE ? ORDER BY id", String.class, "%Dr. " + doctor + " %");
    }

    private JsonNode ok(ResponseEntity<String> res) throws Exception {
        assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody());
    }

    private long book(String patient, long slotId) throws Exception {
        return ok(api.call(HttpMethod.POST, "/appointment/NewAppointment", patient, "{\"slotId\":" + slotId + "}"))
                .get("appointmentID").asLong();
    }

    private void cancel(String patient, long appointmentId) {
        assertThat(api.call(HttpMethod.DELETE, "/appointment/CancelAppointment/" + appointmentId, patient, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private static String wait(long doctorId, LocalDate date, String shift, String more) {
        return "{\"doctorId\":" + doctorId + ",\"date\":\"" + date + "\",\"shift\":" + (shift == null ? "null" : "\"" + shift + "\"")
                + (more == null ? "" : "," + more) + "}";
    }

    @Test
    void aFreedTimeIsToldToEveryoneWaitingForItsShiftAtMostOnceIn30Minutes() throws Exception {
        String desk = api.staff("deskwlt", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drwlt1").id();
        // Tomorrow is full: the morning time and both evening times booked
        String fill1 = api.registerPatient("wltfill1");
        String fill2 = api.registerPatient("wltfill2");
        String fill3 = api.registerPatient("wltfill3");
        long morning = book(fill1, api.freeSlotsTomorrow(fill1, doctorId, "MORNING").get(0).get("id").asLong());
        JsonNode evening = api.freeSlotsTomorrow(fill2, doctorId, "EVENING");
        long evening1 = book(fill2, evening.get(0).get("id").asLong());
        long evening2 = book(fill3, evening.get(1).get("id").asLong());

        String asha = api.registerPatient("wltasha");
        long mira = ok(api.call(HttpMethod.POST, "/api/patient/relative/add", asha,
                "{\"name\":\"Mira Wlt\",\"gender\":\"FEMALE\",\"dob\":\"2018-05-01\",\"relationship\":\"DAUGHTER\"}")).get("id").asLong();
        String ravi = api.registerPatient("wltravi");
        ok(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW, "MORNING", null)));
        long miraWait = ok(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW, null, "\"relativeId\":" + mira))).get("id").asLong();
        ok(api.call(HttpMethod.POST, MINE, ravi, wait(doctorId, TOMORROW, "EVENING", null)));
        // A walk-in record (no login) waiting for either shift, registered with a visit to another doctor
        long other = api.doctorWithSchedule("drwlt2").id();
        long slot = api.get(desk, "/api/receptionist/doctors/" + other + "/next-free-slots?limit=1").get(0).get("slotId").asLong();
        long walkIn = ok(api.call(HttpMethod.POST, "/api/receptionist/walk-in", desk,
                "{\"newPatient\":{\"name\":\"Walk Wlt\",\"phone\":\"9000081001\",\"gender\":\"MALE\"},\"age\":40,\"slotId\":" + slot + "}"))
                .get("patientId").asLong();
        ok(api.call(HttpMethod.POST, DESK, desk, wait(doctorId, TOMORROW, null, "\"patientId\":" + walkIn)));

        // The morning time frees up: the morning's and either shift's waits are told, not the evening's
        cancel(fill1, morning);
        String day = DAY.format(TOMORROW);
        String link = " -> /appointments?doctorId=" + doctorId + "&date=" + TOMORROW + "&shift=MORNING";
        assertThat(bell("wltasha")).containsExactly(
                "WAITLIST_TIME_FREE A time is free: Dr. drwlt1 | " + day + ", 10:00–10:30, for wltasha. First come, first served: book it now." + link,
                "WAITLIST_TIME_FREE A time is free: Dr. drwlt1 | " + day + ", 10:00–10:30, for Mira Wlt. First come, first served: book it now." + link);
        assertThat(bell("wltravi")).isEmpty();
        assertThat(sms("drwlt1")).containsExactly(
                "+919876543210: Hospital ERP: a time with Dr. drwlt1 is free on " + day + " at 10:00 (waitlist for wltasha). First come, first served: book it in the app.",
                "+919876543210: Hospital ERP: a time with Dr. drwlt1 is free on " + day + " at 10:00 (waitlist for Mira Wlt). First come, first served: book it in the app.",
                "9000081001: Hospital ERP: a time with Dr. drwlt1 is free on " + day + " at 10:00 (waitlist for Walk Wlt). First come, first served: contact the front desk.");
        assertThat(api.get(desk, DESK + "?date=" + TOMORROW + "&doctorId=" + doctorId))
                .extracting(e -> e.get("name").asText() + " " + (e.get("toldAt").isNull() ? "not told" : "told"))
                .containsExactly("wltasha told", "Mira Wlt told", "wltravi not told", "Walk Wlt told");

        // An evening time: Ravi; Mira and the walk-in were told minutes ago
        cancel(fill2, evening1);
        assertThat(bell("wltravi")).singleElement().asString().contains("16:00–16:30, for wltravi");
        assertThat(bell("wltasha")).hasSize(2);
        assertThat(sms("drwlt1")).hasSize(4);

        // Half an hour later (for Mira): the last evening time is told to her again, not to the walk-in or Ravi
        jdbc.update("UPDATE waitlist_entries SET told_at = ? WHERE id = ?", LocalDateTime.now().minusMinutes(31), miraWait);
        cancel(fill3, evening2);
        assertThat(bell("wltasha")).hasSize(3).last().asString().contains("16:30–17:00, for Mira Wlt", "&shift=EVENING");
        assertThat(bell("wltravi")).hasSize(1);
        assertThat(sms("drwlt1")).hasSize(5).last().asString().startsWith("+919876543210: ").contains("Mira Wlt");

        // A doctor deactivated: their freed times aren't told; the waits end and are told so
        String nina = api.registerPatient("wltnina");
        ok(api.call(HttpMethod.POST, MINE, nina, wait(other, TOMORROW, "MORNING", null))); // the walk-in's visit fills it
        assertThat(api.call(HttpMethod.PUT, "/api/admin/users/" + userId("drwlt2") + "/deactivate", api.admin(), null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bell("wltnina")).containsExactly("WAITLIST_ENDED Waitlist ended: Dr. drwlt2 | Dr. drwlt2 isn't seeing patients any more,"
                + " so the wait for " + day + " (wltnina) has ended. Please choose another day or doctor. -> /appointments");
        assertThat(sms("drwlt2")).containsExactly("+919876543210: Hospital ERP: Dr. drwlt2 isn't seeing patients any more, so the wait"
                + " for " + day + " (wltnina) has ended. Please choose another day or doctor.");
    }

    @Test
    void aDoctorsApprovedLeaveEndsTheWaitsForThoseDaysAndTellsThem() throws Exception {
        TestApi.Doctor doctor = api.doctorWithSchedule("drwlt3");
        LocalDate dayAfter = TOMORROW.plusDays(1); // one morning time
        String fill = api.registerPatient("wltfill4");
        book(fill, api.get(fill, "/api/slots/available/" + doctor.id() + "?date=" + dayAfter + "&shift=MORNING").get(0).get("id").asLong());
        book(fill, api.freeSlotsTomorrow(fill, doctor.id(), "MORNING").get(0).get("id").asLong()); // and tomorrow: all three
        for (JsonNode time : api.freeSlotsTomorrow(fill, doctor.id(), "EVENING")) {
            book(fill, time.get("id").asLong());
        }
        String vera = api.registerPatient("wltvera");
        ok(api.call(HttpMethod.POST, MINE, vera, wait(doctor.id(), dayAfter, null, null)));
        String zoe = api.registerPatient("wltzoe");
        ok(api.call(HttpMethod.POST, MINE, zoe, wait(doctor.id(), TOMORROW, null, null)));
        String sam = api.registerPatient("wltsam"); // waiting for another doctor that day: goes on
        long other = api.doctorWithSchedule("drwlt4").id();
        book(fill, api.freeSlotsTomorrow(fill, other, "MORNING").get(0).get("id").asLong());
        ok(api.call(HttpMethod.POST, MINE, sam, wait(other, TOMORROW, "MORNING", null)));

        long leave = ok(api.call(HttpMethod.POST, "/api/leaves/apply", api.login("drwlt3", "doctor-123"),
                "{\"startDate\":\"" + TOMORROW + "\",\"endDate\":\"" + dayAfter + "\",\"reason\":\"Conference\"}")).get("id").asLong();
        assertThat(api.call(HttpMethod.PUT, "/api/admin/approve/" + leave, api.admin(), null).getStatusCode()).isEqualTo(HttpStatus.OK);

        String day = DAY.format(dayAfter);
        assertThat(bell("wltvera")).containsExactly("WAITLIST_ENDED Waitlist ended: Dr. drwlt3 | Dr. drwlt3 is away on " + day
                + ", so the wait for " + day + " (wltvera) has ended. Please choose another day or doctor. -> /appointments");
        assertThat(bell("wltzoe")).singleElement().asString().contains("Dr. drwlt3 is away on " + DAY.format(TOMORROW));
        assertThat(sms("drwlt3")).hasSize(2).allSatisfy(text -> assertThat(text).contains("Dr. drwlt3 is away on "));
        assertThat(api.get(vera, MINE + "/my")).isEmpty();
        assertThat(api.get(sam, MINE + "/my")).hasSize(1);
        assertThat(bell("wltsam")).isEmpty();
        assertThat(api.get(api.admin(), DESK + "?date=" + dayAfter + "&doctorId=" + doctor.id()))
                .extracting(e -> e.get("status").asText()).containsExactly("ENDED");
    }

    @Test
    void aTimeThatHasAlreadyStartedIsNotTold() throws Exception {
        String desk = api.staff("deskwlt5", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drwlt5").id();
        String fill = api.registerPatient("wltfill5");
        long visit = book(fill, api.freeSlotsTomorrow(fill, doctorId, "MORNING").get(0).get("id").asLong());
        // The visit was yesterday and nobody came; someone was waiting for that day
        LocalDate yesterday = LocalDate.now().minusDays(1);
        jdbc.update("UPDATE slots SET date = ? WHERE id = (SELECT slot_id FROM appointments WHERE appointmentid = ?)", yesterday, visit);
        jdbc.update("UPDATE appointments SET date = ? WHERE appointmentid = ?", yesterday, visit);
        api.registerPatient("wltuna");
        long una = jdbc.queryForObject("SELECT p.patient_id FROM patient p JOIN users u ON u.id = p.user_id WHERE u.username = 'wltuna'",
                Long.class);
        jdbc.update("INSERT INTO waitlist_entries (patient_id, doctor_id, visit_date, status, joined_at, joined_by)"
                + " VALUES (?, ?, ?, 'WAITING', ?, 'wltuna')", una, doctorId, yesterday, LocalDateTime.now().minusDays(2));

        cancel(desk, visit); // the desk clears the no-show: the time is free again, but over

        assertThat(bell("wltuna")).isEmpty();
        assertThat(sms("drwlt5")).isEmpty();
    }

    @Test
    void waitsAreDeleted30DaysAfterTheirDay() {
        long patient = jdbc.queryForObject("SELECT patient_id FROM patient ORDER BY patient_id LIMIT 1", Long.class);
        long doctor = jdbc.queryForObject("SELECT id FROM doctor ORDER BY id LIMIT 1", Long.class);
        for (int daysAgo : new int[]{31, 30}) {
            jdbc.update("INSERT INTO waitlist_entries (patient_id, doctor_id, visit_date, status, joined_at, joined_by)"
                    + " VALUES (?, ?, ?, 'WAITING', ?, 'wltold')", patient, doctor, LocalDate.now().minusDays(daysAgo), LocalDateTime.now());
        }

        cleanup.deleteOldWaits();

        assertThat(jdbc.queryForList("SELECT visit_date FROM waitlist_entries WHERE joined_by = 'wltold'", LocalDate.class))
                .containsExactly(LocalDate.now().minusDays(30));
    }
}
