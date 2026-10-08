package com.itmonteur.hospitalerp;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Walk-in patient records (docs/WALK_IN_REGISTRATION_PLAN.md, step W.1): no login, phone search across number
 * formats. Same configuration as FeatureFlowH2Test, so both share one application context; the phone numbers
 * (+91 90000 1111x) are used by no other test.
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

    @Autowired private PtInfoService patients;
    @Autowired private AuditLog auditLog;
    @Autowired private TestRestTemplate rest;

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
        assertThat(call(HttpMethod.POST, "/api/admin/patient", login("flowadmin", "flow-admin-123"), """
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

    private String login(String username, String password) {
        ResponseEntity<String> res = call(HttpMethod.POST, "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return res.getBody().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    private ResponseEntity<String> call(HttpMethod method, String url, String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }
}
