package com.itmonteur.hospitalerp;

import com.itmonteur.hospitalerp.audit.AuditAction;
import com.itmonteur.hospitalerp.audit.AuditEntryDTO;
import com.itmonteur.hospitalerp.audit.AuditFilter;
import com.itmonteur.hospitalerp.audit.AuditLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The audit log's recording rules and search (docs/AUDIT_LOG_PLAN.md, step A.1). Same configuration as
 * FeatureFlowH2Test, so both share one application context; patient ids 990_00x are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class AuditLogH2Test {

    @Autowired private AuditLog auditLog;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void logOut() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void writtenOnlyWhenTheTransactionCommits() {
        long patient = 990_001L;
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            auditLog.record(AuditAction.CONSULTATION_SAVED, patient, 1L, "kept");
            assertThat(entriesFor(patient)).as("not before the commit").isEmpty();
        });
        assertThat(entriesFor(patient)).extracting(AuditEntryDTO::getDetails).containsExactly("kept");

        transaction.executeWithoutResult(status -> {
            auditLog.record(AuditAction.CONSULTATION_SAVED, patient, 2L, "rolled back");
            status.setRollbackOnly();
        });
        assertThat(entriesFor(patient)).extracting(AuditEntryDTO::getDetails).containsExactly("kept");
    }

    @Test
    void writtenRightAwayWithoutATransactionAndAfterAReadOnlyOne() {
        long patient = 990_002L;
        auditLog.record(AuditAction.MEDICAL_HISTORY_VIEWED, patient, patient, "no transaction");

        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);
        readOnly.executeWithoutResult(status -> auditLog.record(AuditAction.PRESCRIPTION_DOWNLOADED, patient, 5L, "read-only"));

        assertThat(entriesFor(patient)).extracting(AuditEntryDTO::getDetails)
                .containsExactly("read-only", "no transaction"); // newest first
    }

    @Test
    void recordsWhoWhenAndFromWhere() {
        long patient = 990_003L;
        Long adminId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'flowadmin'", Long.class);
        logIn("flowadmin", "ROLE_ADMIN");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.5"); // nginx in the Docker network
        request.addHeader("X-Real-IP", "203.0.113.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        auditLog.record(AuditAction.PATIENT_PROFILE_UPDATED, patient, patient, "x".repeat(300));

        AuditEntryDTO entry = entriesFor(patient).get(0);
        assertThat(entry.getActorUserId()).isEqualTo(adminId);
        assertThat(entry.getActorUsername()).isEqualTo("flowadmin");
        assertThat(entry.getActorRole()).isEqualTo("ADMIN");
        assertThat(entry.getAction()).isEqualTo(AuditAction.PATIENT_PROFILE_UPDATED);
        assertThat(entry.getTargetType()).isEqualTo(AuditAction.Target.PATIENT);
        assertThat(entry.getTargetId()).isEqualTo(patient);
        assertThat(entry.getPatientId()).isEqualTo(patient);
        assertThat(entry.getIpAddress()).isEqualTo("203.0.113.7");
        assertThat(entry.getDetails()).as("cut to the column size").hasSize(255);
        assertThat(entry.getOccurredAt()).isAfter(before).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void withoutALoginOrARequestThereIsNoActorAndNoAddress() {
        long patient = 990_004L;
        auditLog.record(AuditAction.PATIENT_LIST_VIEWED, patient, null, null);

        AuditEntryDTO entry = entriesFor(patient).get(0);
        assertThat(entry.getActorUserId()).isNull();
        assertThat(entry.getActorUsername()).isNull();
        assertThat(entry.getActorRole()).isNull();
        assertThat(entry.getIpAddress()).isNull();
    }

    @Test
    void aFailedWriteNeverBreaksTheCaller() {
        long patient = 990_005L;
        // A null action breaks the NOT NULL column: the write fails, inside and outside a transaction
        assertThatCode(() -> auditLog.record(null, patient, null, null)).doesNotThrowAnyException();
        assertThatCode(() -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> auditLog.record(null, patient, null, null)))
                .doesNotThrowAnyException();
        assertThat(entriesFor(patient)).isEmpty();
    }

    @Test
    void searchFiltersAndPagesNewestFirst() {
        long patient = 990_006L;
        logIn("flowadmin", "ROLE_ADMIN");
        auditLog.record(AuditAction.CONSULTATION_VIEWED, patient, 1L, null);
        auditLog.record(AuditAction.CONSULTATION_VIEWED, patient, 2L, null);
        auditLog.record(AuditAction.PATIENT_PROFILE_VIEWED, patient, patient, null);
        SecurityContextHolder.clearContext();
        auditLog.record(AuditAction.PATIENT_PROFILE_VIEWED, patient, patient, null);
        LocalDate today = LocalDate.now();

        assertThat(search(new AuditFilter(patient, null, null, null, null), 100).getTotalElements()).isEqualTo(4);
        assertThat(search(new AuditFilter(patient, null, AuditAction.CONSULTATION_VIEWED, null, null), 100)
                .getContent()).extracting(AuditEntryDTO::getTargetId).containsExactly(2L, 1L);
        assertThat(search(new AuditFilter(patient, " flowadmin ", null, null, null), 100).getTotalElements()).isEqualTo(3);
        assertThat(search(new AuditFilter(patient, "nobody", null, null, null), 100).getTotalElements()).isZero();
        assertThat(search(new AuditFilter(patient, null, null, today, today), 100).getTotalElements())
                .as("from and to include the whole day").isEqualTo(4);
        assertThat(search(new AuditFilter(patient, null, null, today.plusDays(1), null), 100).getTotalElements()).isZero();
        assertThat(search(new AuditFilter(patient, null, null, null, today.minusDays(1)), 100).getTotalElements()).isZero();

        Page<AuditEntryDTO> first = auditLog.search(new AuditFilter(patient, null, null, null, null), 0, 3);
        Page<AuditEntryDTO> second = auditLog.search(new AuditFilter(patient, null, null, null, null), 1, 3);
        assertThat(first.getContent()).hasSize(3);
        assertThat(second.getContent()).hasSize(1);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(second.getContent().get(0).getTargetId()).as("the oldest entry comes last").isEqualTo(1L);
        assertThat(search(AuditFilter.none(), 1000).getSize()).as("page size is capped").isEqualTo(100);
    }

    private Page<AuditEntryDTO> search(AuditFilter filter, int size) {
        return auditLog.search(filter, 0, size);
    }

    private List<AuditEntryDTO> entriesFor(long patientId) {
        return search(new AuditFilter(patientId, null, null, null, null), 100).getContent();
    }

    private static void logIn(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(role))));
    }
}
