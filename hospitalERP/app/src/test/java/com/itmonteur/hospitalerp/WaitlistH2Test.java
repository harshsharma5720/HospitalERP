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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The waitlist (docs/WAITLIST_PLAN.md, WL.1) over HTTP: joining and its rules, the patient's and the desk's lists,
 * leaving and removing, and the waits a booking or a deactivation ends. Same configuration as FeatureFlowH2Test, so
 * both share one application context; the accounts *wl* and the phones 9000080xxx are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class WaitlistH2Test {

    private static final String MINE = "/api/waitlist";
    private static final String DESK = "/api/receptionist/waitlist";
    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    private long patientId(String username) {
        return jdbc.queryForObject("SELECT p.patient_id FROM patient p JOIN users u ON u.id = p.user_id WHERE u.username = ?",
                Long.class, username);
    }

    private long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    private static String wait(Long doctorId, LocalDate date, String shift, String more) {
        return "{\"doctorId\":" + doctorId + ",\"date\":\"" + date + "\",\"shift\":" + (shift == null ? "null" : "\"" + shift + "\"")
                + (more == null ? "" : "," + more) + "}";
    }

    private static void refused(ResponseEntity<String> res, HttpStatus status, String message) {
        assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(status);
        assertThat(res.getBody()).contains(message);
    }

    private JsonNode ok(ResponseEntity<String> res) throws Exception {
        assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody());
    }

    private long addRelative(String token, String name) throws Exception {
        return ok(api.call(HttpMethod.POST, "/api/patient/relative/add", token, "{\"name\":\"" + name
                + "\",\"gender\":\"FEMALE\",\"dob\":\"2018-05-01\",\"relationship\":\"DAUGHTER\"}")).get("id").asLong();
    }

    // Books the doctor's one morning time tomorrow (doctorWithSchedule): the morning is full
    private long bookTomorrowMorning(String patient, long doctorId) throws Exception {
        long slotId = api.freeSlotsTomorrow(patient, doctorId, "MORNING").get(0).get("id").asLong();
        return ok(api.call(HttpMethod.POST, "/appointment/NewAppointment", patient, "{\"slotId\":" + slotId + "}"))
                .get("appointmentID").asLong();
    }

    @Test
    void aPatientWaitsForAFullShiftForThemselfOrAFamilyMember() throws Exception {
        long doctorId = api.doctorWithSchedule("drwl1").id();
        String asha = api.registerPatient("wlasha");
        long mira = addRelative(asha, "Mira Wl");
        String ravi = api.registerPatient("wlravi");

        refused(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW, "MORNING", null)), HttpStatus.CONFLICT,
                "There are free times then. Please book one.");
        bookTomorrowMorning(ravi, doctorId);

        JsonNode own = ok(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW, "MORNING", null)));
        assertThat(own.get("name").asText()).isEqualTo("wlasha");
        assertThat(own.get("status").asText()).isEqualTo("WAITING");
        assertThat(own.get("shift").asText()).isEqualTo("MORNING");
        assertThat(own.get("doctorName").asText()).isEqualTo("drwl1");
        assertThat(own.get("joinedBy").asText()).isEqualTo("wlasha");
        assertThat(own.get("toldAt").isNull()).isTrue();
        refused(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW, null, null)), HttpStatus.CONFLICT,
                "There are free times then"); // either shift: the evening still has some
        JsonNode forMira = ok(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW, "MORNING", "\"relativeId\":" + mira)));
        assertThat(forMira.get("name").asText()).isEqualTo("Mira Wl");
        assertThat(forMira.get("patientName").asText()).isEqualTo("wlasha");
        assertThat(forMira.get("relativeId").asLong()).isEqualTo(mira);

        // Joined again: the same wait
        assertThat(ok(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW, "MORNING", null))).get("id").asLong())
                .isEqualTo(own.get("id").asLong());
        jdbc.update("INSERT INTO waitlist_entries (patient_id, doctor_id, visit_date, status, joined_at, joined_by)"
                + " VALUES (?, ?, ?, 'WAITING', ?, 'wlasha')", patientId("wlasha"), doctorId, LocalDate.now().minusDays(1),
                LocalDateTime.now().minusDays(3)); // a day gone by: not shown, not counted
        JsonNode mine = api.get(asha, MINE + "/my");
        assertThat(mine).extracting(e -> e.get("name").asText()).containsExactly("wlasha", "Mira Wl");

        // Days and doctors that can't be waited for
        refused(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW.plusDays(1), "EVENING", null)),
                HttpStatus.BAD_REQUEST, "Dr. drwl1 has no times then");
        refused(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW.plusDays(3), null, null)),
                HttpStatus.BAD_REQUEST, "Dr. drwl1 has no times then");
        refused(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, LocalDate.now().minusDays(1), "MORNING", null)),
                HttpStatus.BAD_REQUEST, "Choose a day from today up to 30 days ahead");
        refused(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, LocalDate.now().plusDays(30), "MORNING", null)),
                HttpStatus.BAD_REQUEST, "Choose a day from today up to 30 days ahead");
        refused(api.call(HttpMethod.POST, MINE, asha, "{\"date\":\"" + TOMORROW + "\"}"), HttpStatus.BAD_REQUEST,
                "Please choose a doctor and a day");
        assertThat(api.call(HttpMethod.POST, MINE, asha, wait(999999L, TOMORROW, "MORNING", null)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        long raviDaughter = addRelative(ravi, "Ravi's girl");
        assertThat(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW, "MORNING", "\"relativeId\":" + raviDaughter))
                .getStatusCode()).as("another account's family member").isEqualTo(HttpStatus.NOT_FOUND);
        refused(api.call(HttpMethod.POST, MINE, ravi, wait(doctorId, TOMORROW, "MORNING", null)), HttpStatus.CONFLICT,
                "wlravi already has an appointment with Dr. drwl1 that day");

        // At most 5 waits per account, the family's included
        long ashaId = patientId("wlasha");
        for (int day = 5; day <= 7; day++) {
            jdbc.update("INSERT INTO waitlist_entries (patient_id, doctor_id, visit_date, status, joined_at, joined_by)"
                    + " VALUES (?, ?, ?, 'WAITING', ?, 'wlasha')", ashaId, doctorId, LocalDate.now().plusDays(day), LocalDateTime.now());
        }
        long jaya = addRelative(asha, "Jaya Wl");
        refused(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW, "MORNING", "\"relativeId\":" + jaya)),
                HttpStatus.CONFLICT, "At most 5 waits at a time for one account");
        assertThat(api.call(HttpMethod.DELETE, MINE + "/" + forMira.get("id").asLong(), asha, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        ok(api.call(HttpMethod.POST, MINE, asha, wait(doctorId, TOMORROW, "MORNING", "\"relativeId\":" + jaya)));

        // Only one's own; only patients
        assertThat(api.call(HttpMethod.DELETE, MINE + "/" + own.get("id").asLong(), ravi, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.get(ravi, MINE + "/my")).isEmpty();
        assertThat(api.call(HttpMethod.GET, MINE + "/my", api.staff("deskwl1", "RECEPTIONIST"), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.call(HttpMethod.GET, MINE + "/my", null, null).getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        assertThat(api.call(HttpMethod.GET, DESK, asha, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // A removed family member's wait goes with them
        assertThat(api.call(HttpMethod.DELETE, "/api/patient/relative/delete/" + jaya, asha, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api.get(asha, MINE + "/my")).extracting(e -> e.get("name").asText()).doesNotContain("Jaya Wl");
    }

    @Test
    void eitherShiftAMovedVisitAndADoctorAway() throws Exception {
        TestApi.Doctor doctor = api.doctorWithSchedule("drwl3");
        LocalDate dayAfter = TOMORROW.plusDays(1); // the doctor works that morning only, one time
        String other = api.registerPatient("wlother");
        String vera = api.registerPatient("wlvera");
        long dayAfterSlot = api.get(other, "/api/slots/available/" + doctor.id() + "?date=" + dayAfter + "&shift=MORNING")
                .get(0).get("id").asLong();
        long otherVisit = ok(api.call(HttpMethod.POST, "/appointment/NewAppointment", other, "{\"slotId\":" + dayAfterSlot + "}"))
                .get("appointmentID").asLong();

        // Joined for the morning, then again for either shift: the same wait, now for either (no evening that day)
        long entry = ok(api.call(HttpMethod.POST, MINE, vera, wait(doctor.id(), dayAfter, "MORNING", null))).get("id").asLong();
        JsonNode either = ok(api.call(HttpMethod.POST, MINE, vera, wait(doctor.id(), dayAfter, null, null)));
        assertThat(either.get("id").asLong()).isEqualTo(entry);
        assertThat(either.get("shift").isNull()).isTrue();

        // Vera moves her visit of tomorrow evening to the freed time: her wait ends as booked
        long eveningSlot = api.freeSlotsTomorrow(vera, doctor.id(), "EVENING").get(0).get("id").asLong();
        long veraVisit = ok(api.call(HttpMethod.POST, "/appointment/NewAppointment", vera, "{\"slotId\":" + eveningSlot + "}"))
                .get("appointmentID").asLong();
        assertThat(api.call(HttpMethod.DELETE, "/appointment/CancelAppointment/" + otherVisit, other, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        ok(api.call(HttpMethod.PUT, "/appointment/update/" + veraVisit, vera, "{\"slotId\":" + dayAfterSlot + "}"));
        assertThat(api.get(api.staff("deskwl3", "RECEPTIONIST"), DESK + "?date=" + dayAfter + "&doctorId=" + doctor.id()))
                .extracting(e -> e.get("name").asText() + " " + e.get("status").asText()).containsExactly("wlvera BOOKED");

        // A day the doctor is away
        LocalDate away = TOMORROW.plusDays(3);
        long leave = ok(api.call(HttpMethod.POST, "/api/leaves/apply", api.login("drwl3", "doctor-123"),
                "{\"startDate\":\"" + away + "\",\"endDate\":\"" + away + "\",\"reason\":\"Conference\"}")).get("id").asLong();
        assertThat(api.call(HttpMethod.PUT, "/api/admin/approve/" + leave, api.admin(), null).getStatusCode()).isEqualTo(HttpStatus.OK);
        refused(api.call(HttpMethod.POST, MINE, vera, wait(doctor.id(), away, null, null)), HttpStatus.BAD_REQUEST,
                "Dr. drwl3 is away that day");
    }

    @Test
    void theFrontDeskPutsAnyPatientOnAWaitlistAndBookingsOrDeactivationsEndWaits() throws Exception {
        String desk = api.staff("deskwl2", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drwl2").id();
        String kiran = api.registerPatient("wlkiran");
        long kiranId = patientId("wlkiran");
        long son = addRelative(kiran, "Kiran's son");
        long booking = bookTomorrowMorning(api.registerPatient("wlfirst"), doctorId);

        // A walk-in record (no login), registered with a visit to another doctor
        long other = api.doctorWithSchedule("drwl2b").id();
        long slot = api.get(desk, "/api/receptionist/doctors/" + other + "/next-free-slots?limit=1").get(0).get("slotId").asLong();
        long walkIn = ok(api.call(HttpMethod.POST, "/api/receptionist/walk-in", desk,
                "{\"newPatient\":{\"name\":\"Walk Wl\",\"phone\":\"9000080001\",\"gender\":\"MALE\"},\"age\":40,\"slotId\":" + slot + "}"))
                .get("patientId").asLong();

        refused(api.call(HttpMethod.POST, DESK, desk, wait(doctorId, TOMORROW, "MORNING", null)), HttpStatus.BAD_REQUEST,
                "Please choose the patient");
        JsonNode first = ok(api.call(HttpMethod.POST, DESK, desk, wait(doctorId, TOMORROW, "MORNING", "\"patientId\":" + walkIn)));
        assertThat(first.get("hasLogin").asBoolean()).isFalse();
        assertThat(first.get("phone").asText()).isEqualTo("9000080001");
        assertThat(first.get("joinedBy").asText()).isEqualTo("deskwl2");
        ok(api.call(HttpMethod.POST, DESK, desk, wait(doctorId, TOMORROW, "MORNING", "\"patientId\":" + kiranId)));
        ok(api.call(HttpMethod.POST, DESK, desk, wait(doctorId, TOMORROW, "MORNING", "\"patientId\":" + kiranId + ",\"relativeId\":" + son)));
        assertThat(api.call(HttpMethod.POST, DESK, desk, wait(doctorId, TOMORROW, "MORNING", "\"patientId\":999999")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // The day's list: by doctor, in the order they joined; the patient sees theirs
        JsonNode day = api.get(desk, DESK + "?date=" + TOMORROW + "&doctorId=" + doctorId);
        assertThat(day).extracting(e -> e.get("name").asText()).containsExactly("Walk Wl", "wlkiran", "Kiran's son");
        assertThat(api.get(api.admin(), DESK + "?date=" + TOMORROW)).extracting(e -> e.get("name").asText())
                .contains("Walk Wl", "wlkiran", "Kiran's son");
        assertThat(api.get(kiran, MINE + "/my")).hasSize(2);

        // Kiran's son is booked (the morning time freed and taken): his wait ends as booked, the others go on
        assertThat(api.call(HttpMethod.DELETE, "/appointment/CancelAppointment/" + booking, api.admin(), null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        long freed = api.freeSlotsTomorrow(kiran, doctorId, "MORNING").get(0).get("id").asLong();
        ok(api.call(HttpMethod.POST, "/appointment/NewAppointment", kiran, "{\"slotId\":" + freed + ",\"relativeId\":" + son + "}"));
        day = api.get(desk, DESK + "?date=" + TOMORROW + "&doctorId=" + doctorId);
        assertThat(day).extracting(e -> e.get("name").asText() + " " + e.get("status").asText())
                .containsExactly("Walk Wl WAITING", "wlkiran WAITING", "Kiran's son BOOKED");
        assertThat(api.get(kiran, MINE + "/my")).extracting(e -> e.get("name").asText()).containsExactly("wlkiran");
        refused(api.call(HttpMethod.DELETE, DESK + "/" + day.get(2).get("id").asLong(), desk, null), HttpStatus.CONFLICT,
                "This wait has already ended");

        // The desk removes one; again: not found
        assertThat(api.call(HttpMethod.DELETE, DESK + "/" + first.get("id").asLong(), desk, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(api.call(HttpMethod.DELETE, DESK + "/" + first.get("id").asLong(), desk, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // A deactivated patient's waits end
        assertThat(api.call(HttpMethod.PUT, "/api/admin/users/" + userId("wlkiran") + "/deactivate", api.admin(), null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api.get(desk, DESK + "?date=" + TOMORROW + "&doctorId=" + doctorId))
                .extracting(e -> e.get("name").asText() + " " + e.get("status").asText())
                .containsExactly("wlkiran ENDED", "Kiran's son BOOKED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inbox_messages WHERE user_id = ? AND kind = 'WAITLIST_ENDED'",
                Integer.class, userId("wlkiran"))).as("a closed account isn't told").isZero();

        // A deactivated doctor's waits end (the walk-in's visit fills the other doctor's morning)
        String nina = api.registerPatient("wlnina");
        ok(api.call(HttpMethod.POST, MINE, nina, wait(other, TOMORROW, "MORNING", null)));
        assertThat(api.call(HttpMethod.PUT, "/api/admin/users/" + userId("drwl2b") + "/deactivate", api.admin(), null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api.get(nina, MINE + "/my")).isEmpty();
        assertThat(api.get(desk, DESK + "?date=" + TOMORROW + "&doctorId=" + other))
                .extracting(e -> e.get("status").asText()).containsExactly("ENDED");
        refused(api.call(HttpMethod.POST, MINE, nina, wait(other, TOMORROW, "MORNING", null)), HttpStatus.BAD_REQUEST,
                "Dr. drwl2b isn't seeing patients");
    }
}
