package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.inbox.Inbox;
import com.itmonteur.hospitalerp.inbox.InboxKind;
import com.itmonteur.hospitalerp.inbox.internal.InboxCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The notification centre's own part (docs/NOTIFICATION_CENTRE_PLAN.md, NC.1): messages written through `Inbox`, read
 * over HTTP. Same configuration as FeatureFlowH2Test, so both share one application context; the accounts *inbox* are
 * used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class InboxH2Test {

    private static final String INBOX = "/api/inbox";

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Inbox inbox;
    @Autowired private InboxCleanup cleanup;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    private long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    private long unread(String token) throws Exception {
        return api.get(token, INBOX + "/unread-count").get("unread").asLong();
    }

    private HttpStatus post(String token, String url) {
        return HttpStatus.valueOf(api.call(HttpMethod.POST, url, token, null).getStatusCode().value());
    }

    @Test
    void eachUserReadsOnlyTheirOwnMessagesAndMarksThemRead() throws Exception {
        String asha = api.registerPatient("inboxasha");
        String ravi = api.registerPatient("inboxravi");
        long ashaId = userId("inboxasha");
        inbox.toUser(ashaId, InboxKind.VISIT_BOOKED, "Appointment booked", "Dr. Anita Rao, Tue 21 Oct, 10:00.", "/appointment-details");
        inbox.toUser(ashaId, InboxKind.BILL_READY, "Your bill is ready", "B-2026-000042: Rs. 500.00.", "/edit-profile");
        inbox.toUser(userId("inboxravi"), InboxKind.VISIT_CANCELLED, "Appointment cancelled", "Ravi's visit.", null);
        inbox.toUser(null, InboxKind.VISIT_BOOKED, "Nobody", "A walk-in without a login gets nothing.", null);

        JsonNode mine = api.get(asha, INBOX);
        assertThat(mine.get("unread").asLong()).isEqualTo(2);
        JsonNode messages = mine.get("messages");
        assertThat(messages).extracting(m -> m.get("kind").asText()).containsExactly("BILL_READY", "VISIT_BOOKED"); // newest first
        JsonNode booked = messages.get(1);
        assertThat(booked.get("title").asText()).isEqualTo("Appointment booked");
        assertThat(booked.get("body").asText()).isEqualTo("Dr. Anita Rao, Tue 21 Oct, 10:00.");
        assertThat(booked.get("link").asText()).isEqualTo("/appointment-details");
        assertThat(booked.get("read").asBoolean()).isFalse();
        assertThat(booked.get("createdAt").asText()).isNotBlank();
        assertThat(api.get(ravi, INBOX).get("messages")).extracting(m -> m.get("title").asText())
                .containsExactly("Appointment cancelled");

        // Someone else's message can't be touched
        long ravisMessage = api.get(ravi, INBOX).get("messages").get(0).get("id").asLong();
        assertThat(post(asha, INBOX + "/" + ravisMessage + "/read")).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unread(ravi)).isEqualTo(1);

        // One read, then all
        JsonNode read = json.readTree(api.call(HttpMethod.POST, INBOX + "/" + booked.get("id").asLong() + "/read", asha, null).getBody());
        assertThat(read.get("read").asBoolean()).isTrue();
        assertThat(unread(asha)).isEqualTo(1);
        assertThat(post(asha, INBOX + "/" + booked.get("id").asLong() + "/read")).as("twice is fine").isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(api.call(HttpMethod.POST, INBOX + "/read-all", asha, null).getBody()).get("read").asInt())
                .isEqualTo(1);
        assertThat(unread(asha)).isZero();
        assertThat(unread(ravi)).as("other users' stay unread").isEqualTo(1);

        assertThat(HttpStatus.valueOf(api.call(HttpMethod.GET, INBOX, null, null).getStatusCode().value()))
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post(null, INBOX + "/read-all")).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void theNewestFiftyOfNinetyDaysLongTextsCutAndOldOnesDeletedAtNight() throws Exception {
        String busy = api.registerPatient("inboxbusy");
        long busyId = userId("inboxbusy");
        for (int i = 1; i <= 55; i++) {
            inbox.toUser(busyId, InboxKind.VISIT_REMINDER, "Reminder " + i, "Tomorrow.", "/appointment-details");
        }
        JsonNode newest = api.get(busy, INBOX);
        assertThat(newest.get("messages")).hasSize(50);
        assertThat(newest.get("messages").get(0).get("title").asText()).isEqualTo("Reminder 55");
        assertThat(newest.get("unread").asLong()).isEqualTo(55);

        // Long texts are cut, a link too long for a page is dropped
        inbox.toUser(busyId, InboxKind.REFUND_GIVEN, "T".repeat(200), "B".repeat(600), "/" + "x".repeat(250));
        JsonNode cut = api.get(busy, INBOX).get("messages").get(0);
        assertThat(cut.get("title").asText()).hasSize(120).endsWith("…");
        assertThat(cut.get("body").asText()).hasSize(500).endsWith("…");
        assertThat(cut.get("link").isNull()).isTrue();

        // 91 days old: no longer counted or shown, and deleted at night; 89 days old: kept
        String old = api.registerPatient("inboxold");
        long oldId = userId("inboxold");
        inbox.toUser(oldId, InboxKind.LEAVE_APPROVED, "Long ago", "91 days.", null);
        inbox.toUser(oldId, InboxKind.LEAVE_REJECTED, "A while ago", "89 days.", null);
        jdbc.update("UPDATE inbox_messages SET created_at = ? WHERE user_id = ? AND title = 'Long ago'",
                LocalDateTime.now().minusDays(91), oldId);
        jdbc.update("UPDATE inbox_messages SET created_at = ? WHERE user_id = ? AND title = 'A while ago'",
                LocalDateTime.now().minusDays(89), oldId);
        assertThat(api.get(old, INBOX).get("messages")).extracting(m -> m.get("title").asText()).containsExactly("A while ago");
        assertThat(unread(old)).isEqualTo(1);
        cleanup.deleteOldMessages();
        assertThat(jdbc.queryForList("SELECT title FROM inbox_messages WHERE user_id = ?", String.class, oldId))
                .containsExactly("A while ago");
    }

    @Test
    void aRoleMessageReachesEveryActiveAccountOfTheRole() throws Exception {
        api.staff("deskinbox1", "RECEPTIONIST");
        api.staff("deskinbox2", "RECEPTIONIST");
        String admin = api.admin();
        assertThat(api.call(HttpMethod.PUT, "/api/admin/users/" + userId("deskinbox2") + "/deactivate", admin, null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        inbox.toRole(Role.RECEPTIONIST, InboxKind.REFUND_DUE, "Refund due: B-2026-000099", "Asha Verma, Rs. 500.00.", "/bills/99");

        String count = "SELECT COUNT(*) FROM inbox_messages WHERE user_id = ? AND kind = 'REFUND_DUE' AND title = 'Refund due: B-2026-000099'";
        assertThat(jdbc.queryForObject(count, Integer.class, userId("deskinbox1"))).isEqualTo(1);
        assertThat(jdbc.queryForObject(count, Integer.class, userId("deskinbox2"))).as("deactivated").isZero();
        assertThat(jdbc.queryForObject(count, Integer.class, userId("flowadmin"))).as("another role").isZero();
        assertThat(api.get(api.login("deskinbox1", "staff-123"), INBOX).get("messages").get(0).get("link").asText())
                .isEqualTo("/bills/99");
    }
}
