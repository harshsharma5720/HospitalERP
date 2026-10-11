package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmonteur.hospitalerp.audit.AuditAction;
import com.itmonteur.hospitalerp.audit.AuditEntryDTO;
import com.itmonteur.hospitalerp.audit.AuditFilter;
import com.itmonteur.hospitalerp.audit.AuditLog;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.Gender;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.patients.PtInfoDTO;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import com.itmonteur.hospitalerp.patients.WalkInPatient;
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
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Walk-in patients at the front desk (docs/WALK_IN_REGISTRATION_PLAN.md): patient records without a login and the
 * phone search (step W.1), the front-desk API - search, next free slots, register-and-book (step W.2). Same
 * configuration as FeatureFlowH2Test, so both share one application context; the phone numbers (+91 90000 111xx)
 * are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class WalkInPatientsH2Test {

    private static final String WALK_IN = "/api/receptionist/walk-in";

    @Autowired private PtInfoService patients;
    @Autowired private AuditLog auditLog;
    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;

    @BeforeEach
    void setUpApi() {
        api = new TestApi(rest, json);
    }

    @Test
    void aWalkInIsAPatientRecordWithoutALogin() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        PtInfo walkIn = patients.registerWalkIn(new WalkInPatient("  Meera Nair ", "+91 90000-11110", Gender.FEMALE,
                LocalDate.of(1980, 5, 1), null));

        assertThat(walkIn.getPatientName()).isEqualTo("Meera Nair");
        assertThat(walkIn.getContactNo()).as("stored without spaces and dashes").isEqualTo("+919000011110");
        assertThat(walkIn.getUser()).as("no login account").isNull();
        assertThat(walkIn.getEmail()).as("no email needed").isNull();
        assertThat(walkIn.getCreatedAt()).isAfter(before).isBefore(LocalDateTime.now().plusSeconds(1));
        // A second one without email: several records may have no email
        assertThat(patients.registerWalkIn(new WalkInPatient("Ravi Nair", "9000011110", Gender.MALE, null, " ")).getEmail())
                .isNull();

        List<AuditEntryDTO> audit = auditLog.search(
                new AuditFilter(walkIn.getPatientId(), null, AuditAction.WALK_IN_REGISTERED, null, null), 0, 10).getContent();
        assertThat(audit).hasSize(1);
        assertThat(audit.get(0).getTargetId()).isEqualTo(walkIn.getPatientId());
    }

    @Test
    void wrongDetailsAreRefused() {
        assertThatThrownBy(() -> patients.registerWalkIn(new WalkInPatient(" ", "9000011112", Gender.MALE, null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("name");
        assertThatThrownBy(() -> patients.registerWalkIn(new WalkInPatient("A", "12345", Gender.MALE, null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("10 to 14 digits");
        assertThatThrownBy(() -> patients.registerWalkIn(new WalkInPatient("A", "9000O11112", Gender.MALE, null, null)))
                .as("a letter O instead of a zero").isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> patients.registerWalkIn(new WalkInPatient("A", "9000011112", null, null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("gender");
        assertThatThrownBy(() -> patients.registerWalkIn(new WalkInPatient("A", "9000011112", Gender.MALE,
                LocalDate.now().plusDays(1), null))).isInstanceOf(BadRequestException.class).hasMessageContaining("future");
        assertThatThrownBy(() -> patients.registerWalkIn(new WalkInPatient("A", "9000011112", Gender.MALE, null, "not-an-email")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("email");
    }

    @Test
    void searchByPhoneFindsTheSameNumberWrittenDifferentlyAndTheWholeFamily() {
        patients.registerWalkIn(new WalkInPatient("Asha Rao", "+91 90000 11111", Gender.FEMALE, null, null));
        patients.registerWalkIn(new WalkInPatient("Kiran Rao", "(900) 001-1111", Gender.MALE, null, null)); // same number
        patients.registerWalkIn(new WalkInPatient("Someone Else", "+919000011119", Gender.OTHER, null, null));
        // A patient who signed up in the app with that number
        assertThat(api.call(HttpMethod.POST, "/api/admin/patient", api.admin(), """
                {"username":"apprao","email":"apprao@example.com","password":"patient-123","phoneNumber":"+919000011111"}""")
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        List<PtInfoDTO> found = patients.findByPhone("9000011111");

        assertThat(found).extracting(PtInfoDTO::getPatientName).containsExactlyInAnyOrder("Asha Rao", "Kiran Rao", "apprao");
        assertThat(found).filteredOn(p -> p.getPatientName().equals("apprao")).allMatch(PtInfoDTO::isHasLogin);
        assertThat(found).filteredOn(p -> !p.getPatientName().equals("apprao")).noneMatch(PtInfoDTO::isHasLogin);
        assertThat(patients.findByPhone("+91 90000 11111")).hasSize(3);
        assertThat(patients.findByPhone("9000011118")).isEmpty();
        assertThatThrownBy(() -> patients.findByPhone("11111")).isInstanceOf(BadRequestException.class);

        List<AuditEntryDTO> searches = auditLog.search(
                new AuditFilter(null, null, AuditAction.PATIENTS_SEARCHED, null, null), 0, 100).getContent();
        assertThat(searches).extracting(AuditEntryDTO::getDetails).contains("by phone ...1111, 3 found", "by phone ...1118, 0 found");
    }

    // ------------------------------------------------------------------ front-desk API (step W.2)

    @Test
    void theFrontDeskRegistersAWalkInAndBooksTheNextFreeSlot() throws Exception {
        String desk = api.staff("deskmeena", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drwalkin").id();
        LocalDate day1 = LocalDate.now().plusDays(1);
        LocalDate day2 = LocalDate.now().plusDays(2);

        // In time order across shifts and days; nothing today (the doctor is off)
        JsonNode next = nextFreeSlots(desk, doctorId, "?limit=4");
        assertThat(slotTimes(next)).containsExactly(day1 + " 10:00 MORNING", day1 + " 16:00 EVENING",
                day1 + " 16:30 EVENING", day2 + " 09:00 MORNING");
        assertThat(slotTimes(nextFreeSlots(desk, doctorId, ""))).as("five by default; the same weekday comes again")
                .hasSize(5).last().isEqualTo(day1.plusWeeks(1) + " 10:00 MORNING");
        assertThat(search(desk, "+91 90000 11120")).as("nobody with this number yet").isEmpty();

        // One request registers the patient and books the first free slot
        ResponseEntity<String> booked = api.call(HttpMethod.POST, WALK_IN, desk, """
                {"newPatient":{"name":"Lakshmi Iyer","phone":"+91 90000 11120","gender":"FEMALE"},
                 "age":52,"slotId":%d,"message":"Fever since yesterday"}""".formatted(next.get(0).get("slotId").asLong()));

        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode result = json.readTree(booked.getBody());
        assertThat(result.get("newPatient").asBoolean()).isTrue();
        long patientId = result.get("patientId").asLong();
        JsonNode appointment = result.get("appointment");
        assertThat(appointment.get("ptInfoId").asLong()).isEqualTo(patientId);
        assertThat(appointment.get("patientName").asText()).isEqualTo("Lakshmi Iyer");
        assertThat(appointment.get("gender").asText()).isEqualTo("FEMALE");
        assertThat(appointment.get("age").asInt()).isEqualTo(52);
        assertThat(appointment.get("doctorId").asLong()).isEqualTo(doctorId);
        assertThat(appointment.get("date").asText()).isEqualTo(day1.toString());
        assertThat(appointment.get("startTime").asText()).startsWith("10:00");
        assertThat(appointment.get("status").asText()).isEqualTo("SCHEDULED");
        assertThat(appointment.get("message").asText()).isEqualTo("Fever since yesterday");

        // The slot is gone, the patient is found by phone next time, the SMS is queued, the audit log has it
        assertThat(slotTimes(nextFreeSlots(desk, doctorId, "?limit=1"))).containsExactly(day1 + " 16:00 EVENING");
        JsonNode found = search(desk, "9000011120");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).get("patientId").asLong()).isEqualTo(patientId);
        assertThat(found.get(0).get("hasLogin").asBoolean()).isFalse();
        assertThat(outboxCount("+919000011120", "booking SMS to patient")).isEqualTo(1);
        assertThat(auditLog.search(new AuditFilter(patientId, null, AuditAction.WALK_IN_REGISTERED, null, null), 0, 10)
                .getContent()).extracting(AuditEntryDTO::getActorUsername).containsExactly("deskmeena");
    }

    @Test
    void aSharedNumberShowsTheFamilyAndBooksForTheOneChosen() throws Exception {
        String desk = api.staff("deskravi", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drfamily").id();
        PtInfo father = patients.registerWalkIn(new WalkInPatient("Arjun Das", "+919000011121", Gender.MALE,
                LocalDate.of(1990, 1, 15), null));
        PtInfo daughter = patients.registerWalkIn(new WalkInPatient("Mira Das", "9000011121", Gender.FEMALE, null, null));

        assertThat(names(search(desk, "+91 90000 11121"))).containsExactlyInAnyOrder("Arjun Das", "Mira Das");
        JsonNode slots = nextFreeSlots(desk, doctorId, "?limit=2");

        // No date of birth on file: the age is needed
        String forDaughter = "\"patientId\":" + daughter.getPatientId() + ",\"slotId\":" + slots.get(0).get("slotId").asLong();
        ResponseEntity<String> withoutAge = api.call(HttpMethod.POST, WALK_IN, desk, "{" + forDaughter + "}");
        assertThat(withoutAge.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(withoutAge.getBody()).contains("age");
        JsonNode forHer = walkIn(desk, "{" + forDaughter + ",\"age\":8}");
        assertThat(forHer.get("newPatient").asBoolean()).isFalse();
        assertThat(forHer.get("patientId").asLong()).isEqualTo(daughter.getPatientId());
        assertThat(forHer.at("/appointment/patientName").asText()).isEqualTo("Mira Das");
        assertThat(forHer.at("/appointment/age").asInt()).isEqualTo(8);

        // The father's age comes from his date of birth
        JsonNode forHim = walkIn(desk, "{\"patientId\":" + father.getPatientId()
                + ",\"slotId\":" + slots.get(1).get("slotId").asLong() + "}");
        assertThat(forHim.at("/appointment/patientName").asText()).isEqualTo("Arjun Das");
        assertThat(forHim.at("/appointment/age").asInt())
                .isEqualTo(Period.between(LocalDate.of(1990, 1, 15), LocalDate.now()).getYears());

        assertThat(patients.findByPhone("9000011121")).as("no new records").hasSize(2);
    }

    @Test
    void aSlotTakenMeanwhileLeavesNoPatientRecordBehind() throws Exception {
        String desk = api.staff("deskasha", "RECEPTIONIST");
        JsonNode slots = nextFreeSlots(desk, api.doctorWithSchedule("drbusy").id(), "?limit=2");
        long slotId = slots.get(0).get("slotId").asLong();
        PtInfo first = patients.registerWalkIn(new WalkInPatient("First Comer", "9000011123", Gender.MALE, null, null));
        walkIn(desk, "{\"patientId\":" + first.getPatientId() + ",\"slotId\":" + slotId + ",\"age\":40}");
        long registeredBefore = auditCount(AuditAction.WALK_IN_REGISTERED);

        ResponseEntity<String> late = api.call(HttpMethod.POST, WALK_IN, desk, """
                {"newPatient":{"name":"Late Comer","phone":"9000011122","gender":"MALE"},"age":30,"slotId":%d}"""
                .formatted(slotId));

        assertThat(late.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(late.getBody()).contains("no longer available");
        assertThat(patients.findByPhone("9000011122")).as("no patient record left behind").isEmpty();
        assertThat(auditCount(AuditAction.WALK_IN_REGISTERED)).as("no audit entry").isEqualTo(registeredBefore);
        assertThat(outboxCount("9000011122", "booking SMS to patient")).as("no SMS").isZero();

        // Wrong requests are refused, and nothing is created either
        long freeSlot = slots.get(1).get("slotId").asLong();
        String newPatient = "\"newPatient\":{\"name\":\"Late Comer\",\"phone\":\"9000011122\",\"gender\":\"MALE\"}";
        assertThat(walkInStatus(desk, "{\"newPatient\":{\"name\":\"Late Comer\",\"phone\":\"12345\",\"gender\":\"MALE\"},"
                + "\"age\":30,\"slotId\":" + freeSlot + "}")).as("phone").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(walkInStatus(desk, "{" + newPatient + ",\"slotId\":" + freeSlot + "}")).as("no age, no date of birth")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(walkInStatus(desk, "{" + newPatient + ",\"age\":131,\"slotId\":" + freeSlot + "}")).as("age")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(walkInStatus(desk, "{" + newPatient + ",\"age\":30}")).as("no slot").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(walkInStatus(desk, "{" + newPatient + ",\"patientId\":" + first.getPatientId() + ",\"age\":30,"
                + "\"slotId\":" + freeSlot + "}")).as("both").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(walkInStatus(desk, "{\"age\":30,\"slotId\":" + freeSlot + "}")).as("neither")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(walkInStatus(desk, "{\"patientId\":987654,\"age\":30,\"slotId\":" + freeSlot + "}"))
                .as("unknown patient").isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(patients.findByPhone("9000011122")).isEmpty();
    }

    @Test
    void onlyTheFrontDeskAndAdminsUseItAndWrongRequestsAreRefused() throws Exception {
        String admin = api.admin();
        TestApi.Doctor doctor = api.doctorWithSchedule("droffwalk");
        String doctorToken = api.login("droffwalk", "doctor-123");
        String patient = api.registerPatient("walkinpeek");
        String slotsUrl = "/api/receptionist/doctors/" + doctor.id() + "/next-free-slots";

        for (String token : List.of(patient, doctorToken)) {
            assertThat(api.call(HttpMethod.GET, "/api/receptionist/patients?phone=9000011120", token, null).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(api.call(HttpMethod.GET, slotsUrl, token, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(walkInStatus(token, "{\"patientId\":1,\"age\":30,\"slotId\":1}")).isEqualTo(HttpStatus.FORBIDDEN);
        }
        // Admins can do what the front desk does
        assertThat(api.call(HttpMethod.GET, "/api/receptionist/patients?phone=9000011120", admin, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(nextFreeSlots(admin, doctor.id(), "?limit=1")).hasSize(1);

        assertThat(api.call(HttpMethod.GET, "/api/receptionist/patients?phone=11120", admin, null).getStatusCode())
                .as("too short").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(api.call(HttpMethod.GET, slotsUrl + "?limit=0", admin, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(api.call(HttpMethod.GET, slotsUrl + "?limit=21", admin, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(api.call(HttpMethod.GET, "/api/receptionist/doctors/987654/next-free-slots", admin, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // A deactivated doctor has no free slots
        assertThat(api.call(HttpMethod.PUT, "/api/admin/users/" + doctor.userId() + "/deactivate", admin, null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(nextFreeSlots(admin, doctor.id(), "")).isEmpty();
    }

    // ------------------------------------------------------------------ helpers

    private JsonNode nextFreeSlots(String token, long doctorId, String query) throws Exception {
        return api.get(token, "/api/receptionist/doctors/" + doctorId + "/next-free-slots" + query);
    }

    // "2026-10-09 10:00 MORNING"
    private static List<String> slotTimes(JsonNode slots) {
        List<String> times = new ArrayList<>();
        slots.forEach(s -> times.add(s.get("date").asText() + " " + s.get("startTime").asText().substring(0, 5)
                + " " + s.get("shift").asText()));
        return times;
    }

    private JsonNode search(String token, String phone) throws Exception {
        return api.get(token, "/api/receptionist/patients?phone=" + phone);
    }

    private static List<String> names(JsonNode patientList) {
        List<String> names = new ArrayList<>();
        patientList.forEach(p -> names.add(p.get("patientName").asText()));
        return names;
    }

    private JsonNode walkIn(String token, String body) throws Exception {
        ResponseEntity<String> res = api.call(HttpMethod.POST, WALK_IN, token, body);
        assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody());
    }

    private HttpStatus walkInStatus(String token, String body) {
        return HttpStatus.valueOf(api.call(HttpMethod.POST, WALK_IN, token, body).getStatusCode().value());
    }

    private long auditCount(AuditAction action) {
        return auditLog.search(new AuditFilter(null, null, action, null, null), 0, 1).getTotalElements();
    }

    private int outboxCount(String recipient, String description) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE recipient = ? AND description = ?",
                Integer.class, recipient, description);
    }
}
