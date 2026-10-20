package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The ABHA number on patients and family members (docs/ABHA_NUMBER_PLAN.md, AB.1) over HTTP: the patient's own edits,
 * the front desk's, walk-ins, one number per person. Same configuration as FeatureFlowH2Test, so both share one
 * application context; the accounts *abha*, the numbers 9100…, and the phones 9000070xxx are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class AbhaNumberH2Test {

    private static final String TAKEN = "This ABHA number is already on another patient's record";
    private static final String WRONG = "An ABHA number has 14 digits, e.g. 12-3456-7890-1234";

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    private long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    private long patientId(String username) {
        return jdbc.queryForObject("SELECT p.patient_id FROM patient p JOIN users u ON u.id = p.user_id WHERE u.username = ?",
                Long.class, username);
    }

    // PUT /api/patient/updateAccount/{userId} is multipart: a JSON part "ptInfoDTO"
    private ResponseEntity<String> updateProfile(String token, long userId, String profileJson) {
        HttpHeaders jsonPart = new HttpHeaders();
        jsonPart.setContentType(MediaType.APPLICATION_JSON);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("ptInfoDTO", new HttpEntity<>(profileJson, jsonPart));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return rest.exchange("/api/patient/updateAccount/" + userId, HttpMethod.PUT, new HttpEntity<>(form, headers), String.class);
    }

    private static void refused(ResponseEntity<String> res, HttpStatus status, String message) {
        assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(status);
        assertThat(res.getBody()).contains(message);
    }

    private ResponseEntity<String> addRelative(String token, String name, String abha) {
        return api.call(HttpMethod.POST, "/api/patient/relative/add", token, "{\"name\":\"" + name + "\",\"gender\":\"FEMALE\","
                + "\"dob\":\"2018-05-01\",\"relationship\":\"DAUGHTER\"" + (abha == null ? "" : ",\"abhaNumber\":\"" + abha + "\"") + "}");
    }

    @Test
    void aPatientSetsTheirOwnAndTheirFamilyMembersAndANumberBelongsToOnePerson() throws Exception {
        String asha = api.registerPatient("abhaasha");
        long ashaUser = userId("abhaasha");

        // Written with dashes, kept as the digits
        assertThat(updateProfile(asha, ashaUser, "{\"abhaNumber\":\"91-0000-0000-0001\"}").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api.get(asha, "/api/patient/getAccount/" + ashaUser).get("abhaNumber").asText()).isEqualTo("91000000000001");
        assertThat(jdbc.queryForList("SELECT details FROM audit_log WHERE action = 'PATIENT_PROFILE_UPDATED' AND patient_id = ? "
                + "ORDER BY id DESC", String.class, patientId("abhaasha")).get(0)).contains("ABHA number").doesNotContain("9100");
        refused(updateProfile(asha, ashaUser, "{\"abhaNumber\":\"91-0000-0000\"}"), HttpStatus.BAD_REQUEST, WRONG);
        assertThat(updateProfile(asha, ashaUser, "{\"patientName\":\"Asha Abha\"}").getStatusCode()).as("not sent: unchanged")
                .isEqualTo(HttpStatus.OK);
        assertThat(api.get(asha, "/api/patient/getAccount/" + ashaUser).get("abhaNumber").asText()).isEqualTo("91000000000001");
        assertThat(updateProfile(asha, ashaUser, "{\"abhaNumber\":\"91000000000001\"}").getStatusCode()).as("their own, again")
                .isEqualTo(HttpStatus.OK);

        // A family member's own number; not one that is on a record already
        ResponseEntity<String> mira = addRelative(asha, "Mira Abha", "91 0000 0000 0002");
        assertThat(mira.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode miraJson = json.readTree(mira.getBody());
        assertThat(miraJson.get("abhaNumber").asText()).isEqualTo("91000000000002");
        refused(addRelative(asha, "Copy Abha", "91-0000-0000-0001"), HttpStatus.CONFLICT, TAKEN);
        long miraId = miraJson.get("id").asLong();
        String miraForm = "{\"name\":\"Mira Abha\",\"gender\":\"FEMALE\",\"dob\":\"2018-05-01\",\"relationship\":\"DAUGHTER\",";
        assertThat(api.call(HttpMethod.PUT, "/api/patient/relative/update/" + miraId, asha, miraForm + "\"abhaNumber\":\"91000000000002\"}")
                .getStatusCode()).as("her own, again").isEqualTo(HttpStatus.OK);

        // Another patient can't take either
        String ravi = api.registerPatient("abharavi");
        refused(updateProfile(ravi, userId("abharavi"), "{\"abhaNumber\":\"91-0000-0000-0002\"}"), HttpStatus.CONFLICT, TAKEN);
        refused(addRelative(ravi, "Ravi's son", "91000000000001"), HttpStatus.CONFLICT, TAKEN);

        // Removed: the number is free again
        assertThat(api.call(HttpMethod.PUT, "/api/patient/relative/update/" + miraId, asha, miraForm + "\"abhaNumber\":\"\"}")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api.get(asha, "/api/patient/relative/" + miraId).get("abhaNumber").isNull()).isTrue();
        assertThat(updateProfile(ravi, userId("abharavi"), "{\"abhaNumber\":\"91-0000-0000-0002\"}").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(updateProfile(asha, ashaUser, "{\"abhaNumber\":\"\"}").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api.get(asha, "/api/patient/getAccount/" + ashaUser).get("abhaNumber").isNull()).isTrue();
    }

    @Test
    void theFrontDeskSetsThemOnThePatientsPageAndAtWalkInRegistration() throws Exception {
        String desk = api.staff("deskabha", "RECEPTIONIST");
        String asha = api.registerPatient("abhadesk");
        long ashaPatient = patientId("abhadesk");
        long sonId = json.readTree(addRelative(asha, "Kabir Abha", null).getBody()).get("id").asLong();
        String patientUrl = "/api/receptionist/patients/" + ashaPatient;

        JsonNode set = json.readTree(api.call(HttpMethod.PUT, patientUrl + "/abha", desk, "{\"abhaNumber\":\"91-0000-0000-0011\"}").getBody());
        assertThat(set.get("abhaNumber").asText()).isEqualTo("91000000000011");
        assertThat(api.get(desk, patientUrl).get("abhaNumber").asText()).as("on the desk's patient page").isEqualTo("91000000000011");
        assertThat(jdbc.queryForObject("SELECT details FROM audit_log WHERE action = 'PATIENT_PROFILE_UPDATED' AND patient_id = ? "
                + "AND actor_username = 'deskabha' ORDER BY id DESC LIMIT 1", String.class, ashaPatient)).isEqualTo("changed: ABHA number");

        JsonNode family = api.get(desk, patientUrl + "/relatives");
        assertThat(family).extracting(r -> r.get("name").asText()).containsExactly("Kabir Abha");
        JsonNode son = json.readTree(api.call(HttpMethod.PUT, patientUrl + "/relatives/" + sonId + "/abha", desk,
                "{\"abhaNumber\":\"91000000000012\"}").getBody());
        assertThat(son.get("abhaNumber").asText()).isEqualTo("91000000000012");
        assertThat(jdbc.queryForObject("SELECT details FROM audit_log WHERE action = 'PATIENT_PROFILE_UPDATED' AND patient_id = ? "
                + "AND actor_username = 'deskabha' ORDER BY id DESC LIMIT 1", String.class, ashaPatient))
                .isEqualTo("changed: a family member's ABHA number");
        refused(api.call(HttpMethod.PUT, patientUrl + "/relatives/" + sonId + "/abha", desk, "{\"abhaNumber\":\"91000000000011\"}"),
                HttpStatus.CONFLICT, TAKEN);
        refused(api.call(HttpMethod.PUT, patientUrl + "/abha", desk, "{\"abhaNumber\":\"91000000000012\"}"), HttpStatus.CONFLICT, TAKEN);
        refused(api.call(HttpMethod.PUT, patientUrl + "/abha", desk, "{\"abhaNumber\":\"abc\"}"), HttpStatus.BAD_REQUEST, WRONG);

        // Another patient's family member isn't reached through this patient
        String other = api.registerPatient("abhaother");
        long otherRelative = json.readTree(addRelative(other, "Other Abha", null).getBody()).get("id").asLong();
        assertThat(api.call(HttpMethod.PUT, patientUrl + "/relatives/" + otherRelative + "/abha", desk, "{\"abhaNumber\":\"91000000000013\"}")
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.call(HttpMethod.GET, "/api/receptionist/patients/999999/relatives", desk, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        // Only the desk and admins
        assertThat(api.call(HttpMethod.PUT, patientUrl + "/abha", asha, "{\"abhaNumber\":\"\"}").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.call(HttpMethod.GET, patientUrl + "/relatives", asha, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.call(HttpMethod.PUT, patientUrl + "/abha", api.admin(), "{\"abhaNumber\":\"91000000000011\"}").getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // A walk-in brings one; a taken one books nothing
        long doctorId = api.doctorWithSchedule("drabha").id();
        JsonNode slots = api.get(desk, "/api/receptionist/doctors/" + doctorId + "/next-free-slots?limit=2");
        ResponseEntity<String> walkIn = api.call(HttpMethod.POST, "/api/receptionist/walk-in", desk,
                "{\"newPatient\":{\"name\":\"Walk Abha\",\"phone\":\"9000070001\",\"gender\":\"MALE\",\"abhaNumber\":\"91-0000-0000-0014\"},"
                        + "\"age\":30,\"slotId\":" + slots.get(0).get("slotId").asLong() + "}");
        assertThat(walkIn.getStatusCode()).as(walkIn.getBody()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("SELECT abha_number FROM patient WHERE patient_name = 'Walk Abha'", String.class))
                .isEqualTo("91000000000014");
        refused(api.call(HttpMethod.POST, "/api/receptionist/walk-in", desk,
                "{\"newPatient\":{\"name\":\"Walk Copy\",\"phone\":\"9000070002\",\"gender\":\"MALE\",\"abhaNumber\":\"91000000000012\"},"
                        + "\"age\":30,\"slotId\":" + slots.get(1).get("slotId").asLong() + "}"), HttpStatus.CONFLICT, TAKEN);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM patient WHERE patient_name = 'Walk Copy'", Integer.class)).isZero();
        assertThat(api.get(desk, "/api/receptionist/doctors/" + doctorId + "/next-free-slots?limit=2").get(0).get("slotId").asLong())
                .as("the slot is still free").isEqualTo(slots.get(1).get("slotId").asLong());
    }
}
