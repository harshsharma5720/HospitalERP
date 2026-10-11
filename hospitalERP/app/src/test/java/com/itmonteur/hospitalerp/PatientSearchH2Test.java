package com.itmonteur.hospitalerp;

import com.itmonteur.hospitalerp.audit.AuditAction;
import com.itmonteur.hospitalerp.audit.AuditEntryDTO;
import com.itmonteur.hospitalerp.audit.AuditFilter;
import com.itmonteur.hospitalerp.audit.AuditLog;
import com.itmonteur.hospitalerp.common.Gender;
import com.itmonteur.hospitalerp.common.PageResult;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.patients.PtInfoDTO;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import com.itmonteur.hospitalerp.patients.WalkInPatient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one-box patient search on a real database (docs/PATIENT_SEARCH_PLAN.md, step P.1). Same configuration as
 * FeatureFlowH2Test, so both share one application context; the names (Quillfeather, Quillon) and phone numbers
 * (+91 90000 222xx) are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class PatientSearchH2Test {

    @Autowired private PtInfoService patients;
    @Autowired private AuditLog auditLog;
    @Autowired private JdbcTemplate jdbc;

    private static List<String> names(PageResult<PtInfoDTO> result) {
        return result.entries().stream().map(PtInfoDTO::getPatientName).toList();
    }

    @Test
    void nameSearchIgnoresCaseTakesTypedWildcardsLiterallyAndPages() {
        for (int i = 23; i >= 1; i--) { // registered in reverse, so the order comes from the sorting
            patients.registerWalkIn(new WalkInPatient("Quillfeather %02d".formatted(i), "90000222%02d".formatted(i),
                    Gender.OTHER, null, null));
        }

        PageResult<PtInfoDTO> first = patients.searchPatients("QUILLFEATHER", 0, 10);
        PageResult<PtInfoDTO> last = patients.searchPatients("quillfeather", 2, 10);

        assertThat(first.totalEntries()).isEqualTo(23);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(names(first)).containsExactly("Quillfeather 01", "Quillfeather 02", "Quillfeather 03",
                "Quillfeather 04", "Quillfeather 05", "Quillfeather 06", "Quillfeather 07", "Quillfeather 08",
                "Quillfeather 09", "Quillfeather 10");
        assertThat(names(last)).containsExactly("Quillfeather 21", "Quillfeather 22", "Quillfeather 23");

        // An underscore or percent sign typed in the box is just that character, not a wildcard
        patients.registerWalkIn(new WalkInPatient("Quillon Ab_c", "9000022299", Gender.OTHER, null, null));
        patients.registerWalkIn(new WalkInPatient("Quillon Abxc", "9000022298", Gender.OTHER, null, null));
        assertThat(names(patients.searchPatients("quillon ab_c", 0, 20))).containsExactly("Quillon Ab_c");
        assertThat(patients.searchPatients("quill%c", 0, 20).totalEntries()).isZero();

        List<AuditEntryDTO> searches = auditLog.search(
                new AuditFilter(null, null, AuditAction.PATIENTS_SEARCHED, null, null), 0, 100).getContent();
        assertThat(searches).extracting(AuditEntryDTO::getDetails)
                .contains("by name \"QUILLFEATHER\", 23 found", "by name \"quill%c\", 0 found");
    }

    @Test
    void byPatientIdAndByPhoneOnTheRealData() {
        PtInfo walkIn = patients.registerWalkIn(new WalkInPatient("Quillon Byid", "+91 90000 22297", Gender.FEMALE,
                null, null));

        assertThat(names(patients.searchPatients("#" + walkIn.getPatientId(), 0, 20))).containsExactly("Quillon Byid");
        assertThat(names(patients.searchPatients("9000022297", 0, 20))).containsExactly("Quillon Byid");
        assertThat(patients.searchPatients("987654", 0, 20).totalEntries()).isZero();

        List<AuditEntryDTO> forPatient = auditLog.search(
                new AuditFilter(walkIn.getPatientId(), null, AuditAction.PATIENTS_SEARCHED, null, null), 0, 10).getContent();
        assertThat(forPatient).extracting(AuditEntryDTO::getDetails)
                .containsExactly("by patient ID " + walkIn.getPatientId() + ", 1 found");
    }

    @Test
    void anEmptyBoxPagesThroughEveryone() {
        long everyone = jdbc.queryForObject("SELECT COUNT(*) FROM patient", Long.class);

        PageResult<PtInfoDTO> first = patients.searchPatients("", 0, 5);
        PageResult<PtInfoDTO> second = patients.searchPatients(null, 1, 5);

        assertThat(first.totalEntries()).isEqualTo(everyone);
        assertThat(first.entries()).hasSize((int) Math.min(5, everyone));
        Set<Long> ids = new HashSet<>();
        first.entries().forEach(p -> ids.add(p.getPatientId()));
        second.entries().forEach(p -> assertThat(ids.add(p.getPatientId())).as("pages don't overlap").isTrue());
        assertThat(auditLog.search(new AuditFilter(null, null, AuditAction.PATIENT_LIST_VIEWED, null, null), 0, 100)
                .getContent()).extracting(AuditEntryDTO::getDetails).anyMatch(d -> d.startsWith("page 1 of "));
    }
}
