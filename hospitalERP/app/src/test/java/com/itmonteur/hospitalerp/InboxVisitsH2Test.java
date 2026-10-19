package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmonteur.hospitalerp.appointments.internal.AppointmentReminderService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visits and leave in the notification centre (docs/NOTIFICATION_CENTRE_PLAN.md, NC.2), from the real flows. Same
 * configuration as FeatureFlowH2Test, so both share one application context; the accounts *inbox* and the phone numbers
 * 9000069xxx are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class InboxVisitsH2Test {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AppointmentReminderService reminders;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    private long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    // A user's messages, oldest first, as "KIND title | body -> link"
    private List<String> messages(long userId) {
        return jdbc.queryForList("SELECT CONCAT(kind, ' ', title, ' | ', body, ' -> ', COALESCE(link, '')) FROM inbox_messages "
                + "WHERE user_id = ? ORDER BY id", String.class, userId);
    }

    private static String slotTime(JsonNode slot) {
        return slot.get("startTime").asText().substring(0, 5) + "–" + slot.get("endTime").asText().substring(0, 5);
    }

    private long book(String patient, long slotId, Long relativeId) throws Exception {
        ResponseEntity<String> booked = api.call(HttpMethod.POST, "/appointment/NewAppointment", patient,
                "{\"slotId\":" + slotId + (relativeId == null ? "" : ",\"relativeId\":" + relativeId) + "}");
        assertThat(booked.getStatusCode()).as(booked.getBody()).isEqualTo(HttpStatus.OK);
        return json.readTree(booked.getBody()).get("appointmentID").asLong();
    }

    @Test
    void aVisitBookedMovedRemindedAndCancelledReachesThePatientAndTheDoctor() throws Exception {
        TestApi.Doctor doctor = api.doctorWithSchedule("drinbox");
        String patient = api.registerPatient("inboxpatient");
        long patientUser = userId("inboxpatient");
        String tomorrow = LocalDate.now().plusDays(1).format(DAY);
        JsonNode evening = api.freeSlotsTomorrow(patient, doctor.id(), "EVENING");
        String first = slotTime(evening.get(0)), second = slotTime(evening.get(1));

        long visit = book(patient, evening.get(0).get("id").asLong(), null);
        assertThat(messages(patientUser)).containsExactly("VISIT_BOOKED Appointment booked | Dr. drinbox, " + tomorrow + ", "
                + first + ", for inboxpatient. -> /appointment-details");
        assertThat(messages(doctor.userId())).containsExactly("VISIT_BOOKED New appointment | inboxpatient, " + tomorrow + ", "
                + first + ". -> /doctor/doctor-appointments");

        // Moved to the next slot: both told the new time
        assertThat(api.call(HttpMethod.PUT, "/appointment/update/" + visit, patient,
                "{\"slotId\":" + evening.get(1).get("id").asLong() + "}").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(messages(patientUser)).last().isEqualTo("VISIT_MOVED Appointment moved | Now Dr. drinbox, " + tomorrow + ", "
                + second + ", for inboxpatient. -> /appointment-details");
        assertThat(messages(doctor.userId())).last().isEqualTo("VISIT_MOVED Appointment moved | inboxpatient: now " + tomorrow
                + ", " + second + ". -> /doctor/doctor-appointments");

        // The day before: the patient only
        reminders.sendDayBeforeReminders();
        assertThat(messages(patientUser)).last().isEqualTo("VISIT_REMINDER Your appointment is tomorrow | Dr. drinbox, "
                + tomorrow + ", " + second + ", for inboxpatient. -> /appointment-details");
        assertThat(messages(doctor.userId())).hasSize(2);

        // Cancelled by the patient
        assertThat(api.call(HttpMethod.DELETE, "/appointment/CancelAppointment/" + visit, patient, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(messages(patientUser)).hasSize(4).last().asString().startsWith("VISIT_CANCELLED Appointment cancelled | Dr. drinbox");
        assertThat(messages(doctor.userId())).hasSize(3).last().asString()
                .startsWith("VISIT_CANCELLED Appointment cancelled | inboxpatient, " + tomorrow);

        // A family member's visit goes to the account holder, with the family member's name
        long daughter = json.readTree(api.call(HttpMethod.POST, "/api/patient/relative/add", patient,
                "{\"name\":\"Mira Inbox\",\"gender\":\"FEMALE\",\"dob\":\"2018-05-01\",\"relationship\":\"DAUGHTER\"}")
                .getBody()).get("id").asLong();
        book(patient, api.freeSlotsTomorrow(patient, doctor.id(), "MORNING").get(0).get("id").asLong(), daughter);
        assertThat(messages(patientUser)).last().asString().contains("for Mira Inbox.");

        // Walk-ins without a login: only the doctor is told
        String desk = api.staff("deskinboxvisits", "RECEPTIONIST");
        TestApi.Doctor walkInDoctor = api.doctorWithSchedule("drinboxwalk");
        api.visitsToday(desk, walkInDoctor.id(), "Inboxwalk", "90000691", jdbc);
        assertThat(messages(walkInDoctor.userId())).hasSize(3).allSatisfy(m -> assertThat(m).startsWith("VISIT_BOOKED New appointment | Inboxwalk"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inbox_messages WHERE body LIKE '%Inboxwalk%'", Integer.class))
                .as("no patient account to tell").isEqualTo(3);
        assertThat(messages(userId("deskinboxvisits"))).as("the desk who booked them").isEmpty();
    }

    @Test
    void aLeaveRequestReachesTheAdminsAndTheDecisionTheDoctorAndTheirPatients() throws Exception {
        TestApi.Doctor doctor = api.doctorWithSchedule("drinboxleave");
        String doctorToken = api.login("drinboxleave", "doctor-123");
        String patient = api.registerPatient("inboxleavepatient");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        JsonNode morning = api.freeSlotsTomorrow(patient, doctor.id(), "MORNING");
        book(patient, morning.get(0).get("id").asLong(), null);
        long admin = userId("flowadmin");
        String leaveMessages = "SELECT CONCAT(kind, ' ', title, ' | ', body, ' -> ', link) FROM inbox_messages "
                + "WHERE user_id = ? AND title LIKE '%drinboxleave%' ORDER BY id";

        ResponseEntity<String> applied = api.call(HttpMethod.POST, "/api/leaves/apply", doctorToken,
                "{\"startDate\":\"" + tomorrow + "\",\"endDate\":\"" + tomorrow + "\",\"reason\":\"Conference\"}");
        assertThat(applied.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForList(leaveMessages, String.class, admin)).containsExactly(
                "LEAVE_REQUESTED Leave request: drinboxleave | " + tomorrow.format(DAY) + ": Conference -> /admin/leave-approval");

        assertThat(api.call(HttpMethod.PUT, "/api/admin/approve/" + json.readTree(applied.getBody()).get("id").asLong(),
                api.admin(), null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(messages(doctor.userId())).last().isEqualTo("LEAVE_APPROVED Your leave is approved | " + tomorrow.format(DAY)
                + ". Appointments on those days are cancelled and the patients told. -> /doctor/leave-management");
        assertThat(messages(userId("inboxleavepatient"))).last().asString()
                .startsWith("VISIT_CANCELLED_BY_LEAVE Appointment cancelled: the doctor is away | Dr. drinboxleave is on leave on "
                        + tomorrow.format(DAY) + ". The visit at " + slotTime(morning.get(0)))
                .endsWith(" -> /appointments");

        // Several days, rejected
        LocalDate later = LocalDate.now().plusDays(10);
        ResponseEntity<String> second = api.call(HttpMethod.POST, "/api/leaves/apply", doctorToken,
                "{\"startDate\":\"" + later + "\",\"endDate\":\"" + later.plusDays(2) + "\",\"reason\":\" Family wedding \"}");
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForList(leaveMessages, String.class, admin)).hasSize(2).last().asString()
                .contains("| " + (later.getMonth() == later.plusDays(2).getMonth()
                        ? later.getDayOfMonth() + "–" + later.plusDays(2).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
                        : later.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)) + " – "
                        + later.plusDays(2).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))) + ": Family wedding -> ");
        assertThat(api.call(HttpMethod.PUT, "/api/admin/reject/" + json.readTree(second.getBody()).get("id").asLong(),
                api.admin(), null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(messages(doctor.userId())).last().asString().startsWith("LEAVE_REJECTED Your leave was not approved | ");
    }
}
