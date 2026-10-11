package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmonteur.hospitalerp.audit.AuditAction;
import com.itmonteur.hospitalerp.audit.AuditEntryDTO;
import com.itmonteur.hospitalerp.audit.AuditFilter;
import com.itmonteur.hospitalerp.audit.AuditLog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The front desk's patient search, patient details and appointment list over HTTP (docs/PATIENT_SEARCH_PLAN.md,
 * step P.2). Same configuration as FeatureFlowH2Test, so both share one application context; the surname
 * "Listwell" and the phone numbers +91 90000 333xx are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrontDeskListsH2Test {

    private static final String APPOINTMENTS = "/api/receptionist/appointments";

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuditLog auditLog;

    private TestApi api;
    private String desk;
    private long doctorId;
    private LocalDate day1;
    private final Map<String, Long> appointmentIds = new HashMap<>(); // by first name
    private final Map<String, Long> patientIds = new HashMap<>();

    /**
     * Four walk-in visits with one doctor: Ana tomorrow 10:00, Ben tomorrow 16:00 (cancelled), Cara the day after
     * 09:00 (completed) and Dev a week after tomorrow 10:00 (completed the old way: only the is_completed flag).
     */
    @BeforeAll
    void bookFourVisits() throws Exception {
        api = new TestApi(rest, json);
        desk = api.staff("desklists", "RECEPTIONIST");
        doctorId = api.doctorWithSchedule("drlists").id();
        day1 = LocalDate.now().plusDays(1);
        // tomorrow 10:00, 16:00, 16:30, the day after 09:00, a week after tomorrow 10:00
        JsonNode slots = api.get(desk, "/api/receptionist/doctors/" + doctorId + "/next-free-slots");
        book("Ana", 1, slots.get(0));
        book("Ben", 2, slots.get(1));
        book("Cara", 3, slots.get(3));
        book("Dev", 4, slots.get(4));
        jdbc.update("UPDATE appointments SET status = 'CANCELLED_BY_PATIENT' WHERE appointmentid = ?", appointmentIds.get("Ben"));
        jdbc.update("UPDATE appointments SET status = 'COMPLETED', is_completed = TRUE WHERE appointmentid = ?",
                appointmentIds.get("Cara"));
        jdbc.update("UPDATE appointments SET is_completed = TRUE WHERE appointmentid = ?", appointmentIds.get("Dev"));
    }

    private void book(String firstName, int phone, JsonNode slot) throws Exception {
        ResponseEntity<String> res = api.call(HttpMethod.POST, "/api/receptionist/walk-in", desk,
                "{\"newPatient\":{\"name\":\"" + firstName + " Listwell\",\"phone\":\"90000333%02d\",\"gender\":\"OTHER\"},"
                        .formatted(phone) + "\"age\":30,\"slotId\":" + slot.get("slotId").asLong() + "}");
        assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(HttpStatus.OK);
        JsonNode booked = json.readTree(res.getBody());
        appointmentIds.put(firstName, booked.at("/appointment/appointmentID").asLong());
        patientIds.put(firstName, booked.get("patientId").asLong());
    }

    // First names of a page of appointments, in order
    private List<String> visits(String query) throws Exception {
        List<String> names = new ArrayList<>();
        api.get(desk, APPOINTMENTS + query).get("entries")
                .forEach(a -> names.add(a.get("patientName").asText().replace(" Listwell", "")));
        return names;
    }

    private HttpStatus status(String token, String url) {
        return HttpStatus.valueOf(api.call(HttpMethod.GET, url, token, null).getStatusCode().value());
    }

    @Test
    void theAppointmentListFiltersByDateDoctorStatusAndPatient() throws Exception {
        String ofDoctor = "?doctorId=" + doctorId;

        assertThat(visits(ofDoctor)).as("by date and time").containsExactly("Ana", "Ben", "Cara", "Dev");
        assertThat(visits(ofDoctor + "&order=desc")).containsExactly("Dev", "Cara", "Ben", "Ana");
        assertThat(visits(ofDoctor + "&from=" + day1 + "&to=" + day1)).containsExactly("Ana", "Ben");
        assertThat(visits(ofDoctor + "&from=" + day1.plusDays(1))).containsExactly("Cara", "Dev");
        assertThat(visits(ofDoctor + "&status=upcoming")).containsExactly("Ana");
        assertThat(visits(ofDoctor + "&status=CANCELLED")).containsExactly("Ben");
        assertThat(visits(ofDoctor + "&status=completed")).as("also the old is_completed flag").containsExactly("Cara", "Dev");
        assertThat(visits(ofDoctor + "&status=all")).hasSize(4);
        // By patient: part of the name (any doctor), or one patient
        assertThat(visits("?patient=LISTWELL")).containsExactly("Ana", "Ben", "Cara", "Dev");
        assertThat(visits("?patient=ben list")).containsExactly("Ben");
        assertThat(visits("?patient=_istwell")).as("a typed underscore is no wildcard").isEmpty();
        assertThat(visits("?patientId=" + patientIds.get("Cara"))).containsExactly("Cara");

        JsonNode secondPage = api.get(desk, APPOINTMENTS + ofDoctor + "&size=3&page=1");
        assertThat(secondPage.get("entries")).hasSize(1);
        assertThat(secondPage.at("/entries/0/patientName").asText()).isEqualTo("Dev Listwell");
        assertThat(secondPage.get("totalEntries").asLong()).isEqualTo(4);
        assertThat(secondPage.get("totalPages").asInt()).isEqualTo(2);
        assertThat(secondPage.get("page").asInt()).isEqualTo(1);
        JsonNode ana = api.get(desk, APPOINTMENTS + ofDoctor + "&size=1").at("/entries/0");
        assertThat(ana.get("startTime").asText()).startsWith("10:00");
        assertThat(ana.get("doctorId").asLong()).isEqualTo(doctorId);
        assertThat(ana.get("status").asText()).isEqualTo("SCHEDULED");

        // A visit whose slot row is gone (old data) is still listed
        jdbc.update("UPDATE appointments SET slot_id = NULL WHERE appointmentid = ?", appointmentIds.get("Ana"));
        assertThat(visits(ofDoctor)).containsExactly("Ana", "Ben", "Cara", "Dev");
    }

    @Test
    void wrongFiltersAreRefused() {
        assertThat(status(desk, APPOINTMENTS + "?status=missed")).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status(desk, APPOINTMENTS + "?from=" + day1 + "&to=" + day1.minusDays(1))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status(desk, APPOINTMENTS + "?order=sideways")).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status(desk, APPOINTMENTS + "?size=101")).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status(desk, APPOINTMENTS + "?page=-1")).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status(desk, APPOINTMENTS + "?from=tomorrow")).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void patientSearchAndOnePatientsDetails() throws Exception {
        JsonNode found = api.get(desk, "/api/receptionist/patients/search?q=listwell&size=3");
        List<String> names = new ArrayList<>();
        found.get("entries").forEach(p -> names.add(p.get("patientName").asText()));
        assertThat(names).containsExactly("Ana Listwell", "Ben Listwell", "Cara Listwell");
        assertThat(found.get("totalEntries").asLong()).isEqualTo(4);
        assertThat(api.get(desk, "/api/receptionist/patients/search?q=" + patientIds.get("Dev")).at("/entries/0/patientName")
                .asText()).isEqualTo("Dev Listwell");
        assertThat(api.get(desk, "/api/receptionist/patients/search?q=9000033302").at("/entries/0/patientName").asText())
                .isEqualTo("Ben Listwell");
        assertThat(status(desk, "/api/receptionist/patients/search?q=listwell&size=0")).isEqualTo(HttpStatus.BAD_REQUEST);

        long cara = patientIds.get("Cara");
        JsonNode details = api.get(desk, "/api/receptionist/patients/" + cara);
        assertThat(details.get("patientName").asText()).isEqualTo("Cara Listwell");
        assertThat(details.get("contactNo").asText()).isEqualTo("9000033303");
        assertThat(details.get("hasLogin").asBoolean()).isFalse();
        assertThat(auditLog.search(new AuditFilter(cara, null, AuditAction.PATIENT_PROFILE_VIEWED, null, null), 0, 10)
                .getContent()).extracting(AuditEntryDTO::getActorUsername).containsExactly("desklists");
        assertThat(status(desk, "/api/receptionist/patients/987654")).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void onlyTheFrontDeskAndAdminsSeeThem() {
        String patient = api.registerPatient("listpeek");
        String doctor = api.login("drlists", "doctor-123");
        List<String> urls = List.of("/api/receptionist/patients/search?q=listwell",
                "/api/receptionist/patients/" + patientIds.get("Ana"), APPOINTMENTS + "?doctorId=" + doctorId);
        for (String url : urls) {
            assertThat(status(patient, url)).as(url).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(status(doctor, url)).as(url).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(status(api.admin(), url)).as(url).isEqualTo(HttpStatus.OK);
        }
    }
}
