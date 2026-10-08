package com.itmonteur.hospitalerp;

import com.itmonteur.hospitalerp.notifications.EmailService;
import com.itmonteur.hospitalerp.notifications.NotificationOutbox;
import com.itmonteur.hospitalerp.notifications.OutboxChannel;
import com.itmonteur.hospitalerp.notifications.OutboxStatus;
import com.itmonteur.hospitalerp.notifications.SmsService;
import com.itmonteur.hospitalerp.notifications.internal.OutboxMessage;
import com.itmonteur.hospitalerp.notifications.internal.OutboxRepository;
import com.itmonteur.hospitalerp.notifications.internal.OutboxSender;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * The notification outbox (docs/RELIABLE_NOTIFICATIONS_PLAN.md, step C.1): queueing in the caller's transaction,
 * delivery, the retry schedule, giving up, skipped channels, claims, and the clean-up. Email and SMS are mocks;
 * the clock is moved by hand; there is no background scheduler, the test runs the sender itself.
 */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:outbox", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Import({NotificationOutbox.class, OutboxSender.class, NotificationOutboxTest.TestClock.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED) // the code under test commits its own transactions
class NotificationOutboxTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 7, 9, 0);

    @Autowired private NotificationOutbox outbox;
    @Autowired private OutboxSender sender;
    @Autowired private OutboxRepository repository;
    @Autowired private MutableClock clock;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private EmailService emailService;
    @MockitoBean private SmsService smsService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        clock.set(START);
        when(emailService.isEnabled()).thenReturn(true);
        when(smsService.isEnabled()).thenReturn(true);
    }

    @Test
    void queuedMessagesAreDeliveredAndMarkedSent() throws Exception {
        inTransaction(() -> {
            outbox.queueEmail("asha@example.com", "Appointment booked", "<p>Hi</p>", "booking email to patient", null);
            outbox.queueSms("+919800000001", "Booked for tomorrow", "booking SMS to patient", null);
        });
        verifyNoInteractions(emailService, smsService); // nothing is sent while queueing

        assertThat(sender.sendDueMessages()).isEqualTo(2);

        verify(emailService).sendHtml("asha@example.com", "Appointment booked", "<p>Hi</p>");
        verify(smsService).send("+919800000001", "Booked for tomorrow");
        assertThat(messages()).allSatisfy(m -> {
            assertThat(m.getStatus()).isEqualTo(OutboxStatus.SENT);
            assertThat(m.getAttempts()).isEqualTo(1);
            assertThat(m.getSentAt()).isEqualTo(START);
            assertThat(m.getLastError()).isNull();
        });
        assertThat(sender.sendDueMessages()).as("nothing is sent twice").isZero();
    }

    @Test
    void aFailedSendIsRetriedWithGrowingGapsUntilItGivesUp() {
        doThrow(new RuntimeException("Twilio is down")).when(smsService).send(anyString(), anyString());
        inTransaction(() -> outbox.queueSms("+919800000001", "Reminder", "reminder SMS", START.plusHours(3)));

        List<Long> attemptMinutes = new ArrayList<>();
        while (only().getStatus() != OutboxStatus.FAILED && attemptMinutes.size() < 20) {
            LocalDateTime due = only().getNextAttemptAt();
            clock.set(due.minusSeconds(1));
            assertThat(sender.sendDueMessages()).as("not before its time").isZero();
            clock.set(due);
            attemptMinutes.add(Duration.between(START, due).toMinutes());
            assertThat(sender.sendDueMessages()).isEqualTo(1);
        }

        // gaps of 1, 5, 15, 30, 60, 60 minutes; the next one (at 231) would be after the 3 hours
        assertThat(attemptMinutes).containsExactly(0L, 1L, 6L, 21L, 51L, 111L, 171L);
        OutboxMessage failed = only();
        assertThat(failed.getAttempts()).isEqualTo(7);
        assertThat(failed.getLastError()).isEqualTo("RuntimeException: Twilio is down");
        verify(smsService, times(7)).send("+919800000001", "Reminder");
    }

    @Test
    void aMessageThatFailsOnceIsDeliveredOnTheRetry() throws Exception {
        doThrow(new MessagingException("SMTP server unreachable")).doNothing()
                .when(emailService).sendHtml(anyString(), anyString(), anyString());
        inTransaction(() -> outbox.queueEmail("asha@example.com", "Cancelled", "<p>x</p>", "cancellation email", null));

        sender.sendDueMessages();
        assertThat(only().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(only().getLastError()).isEqualTo("MessagingException: SMTP server unreachable");

        clock.set(START.plusMinutes(1));
        sender.sendDueMessages();
        assertThat(only().getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(only().getAttempts()).isEqualTo(2);
        assertThat(only().getLastError()).isNull();
    }

    @Test
    void retriesStopAfter24HoursOrAtTheAppointmentButTheFirstAttemptIsAlwaysMade() {
        inTransaction(() -> {
            outbox.queueSms("+911", "a", "no limit given", null);
            outbox.queueSms("+912", "b", "appointment in 3 days", START.plusDays(3));
            outbox.queueSms("+913", "c", "appointment in 2 hours", START.plusHours(2));
            outbox.queueSms("+914", "d", "appointment already over", START.minusHours(1));
        });
        assertThat(messages()).extracting(OutboxMessage::getGiveUpAt).containsExactly(
                START.plusHours(24), START.plusHours(24), START.plusHours(2), START.minusHours(1));

        doThrow(new RuntimeException("down")).when(smsService).send(anyString(), anyString());
        assertThat(sender.sendDueMessages()).as("every message gets its first attempt").isEqualTo(4);
        assertThat(messages()).extracting(OutboxMessage::getStatus).containsExactly(
                OutboxStatus.PENDING, OutboxStatus.PENDING, OutboxStatus.PENDING, OutboxStatus.FAILED);
    }

    @Test
    void aChannelThatIsNotConfiguredIsSkippedNotRetried() throws Exception {
        when(emailService.isEnabled()).thenReturn(false);
        when(smsService.isEnabled()).thenReturn(false);
        inTransaction(() -> {
            outbox.queueEmail("asha@example.com", "s", "<p>x</p>", "email", null);
            outbox.queueSms("+919800000001", "x", "SMS", null);
        });

        sender.sendDueMessages();

        assertThat(messages()).extracting(OutboxMessage::getStatus).containsOnly(OutboxStatus.SKIPPED);
        assertThat(messages()).extracting(OutboxMessage::getLastError).containsExactly(
                "Email is not configured (no SMTP host)", "SMS is not configured (no Twilio account)");
        verify(emailService, never()).sendHtml(any(), any(), any());
        verify(smsService, never()).send(any(), any());
        clock.set(START.plusHours(2));
        assertThat(sender.sendDueMessages()).isZero();
    }

    @Test
    void twoSendersNeverDeliverTheSameMessage() throws Exception {
        CountDownLatch sending = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            sending.countDown();
            release.await(5, TimeUnit.SECONDS);
            return null;
        }).when(emailService).sendHtml(anyString(), anyString(), anyString());
        inTransaction(() -> outbox.queueEmail("asha@example.com", "s", "<p>x</p>", "email", null));

        CompletableFuture<Integer> first = CompletableFuture.supplyAsync(sender::sendDueMessages);
        assertThat(sending.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(sender.sendDueMessages()).as("claimed by the first sender").isZero();
        release.countDown();

        assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo(1);
        verify(emailService, times(1)).sendHtml(anyString(), anyString(), anyString());
        assertThat(only().getStatus()).isEqualTo(OutboxStatus.SENT);
    }

    @Test
    void aMessageClaimedByASenderThatDiedIsSentAgainAfterTheLease() throws Exception {
        inTransaction(() -> outbox.queueEmail("asha@example.com", "s", "<p>x</p>", "email", null));
        Long id = only().getId();
        // Two senders that both found the message due: only the first claim succeeds
        List<Integer> claims = new ArrayList<>();
        inTransaction(() -> claims.add(repository.claim(id, START, START.plusMinutes(10))));
        inTransaction(() -> claims.add(repository.claim(id, START, START.plusMinutes(10))));
        assertThat(claims).containsExactly(1, 0);
        // ... and the sender that got it died before finishing

        assertThat(sender.sendDueMessages()).isZero();
        clock.set(START.plusMinutes(10));
        assertThat(sender.sendDueMessages()).isEqualTo(1);
        assertThat(only().getStatus()).isEqualTo(OutboxStatus.SENT);
        verify(emailService, times(1)).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void aRolledBackChangeLeavesNoMessageAndAMissingAddressIsNotQueued() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            outbox.queueSms("+919800000001", "x", "booking SMS", null);
            status.setRollbackOnly();
        });
        inTransaction(() -> {
            outbox.queueSms(null, "x", "no phone number", null);
            outbox.queueEmail(" ", "s", "<p>x</p>", "no email address", null);
        });

        assertThat(repository.count()).isZero();
    }

    @Test
    void theCleanUpKeepsFailedMessagesLongerAndNeverDeletesUnsentOnes() {
        doThrow(new RuntimeException("down")).when(smsService).send(eq("+91fail"), anyString());
        clock.set(START.minusDays(91));
        inTransaction(() -> outbox.queueSms("+91fail", "x", "failed 91 days ago", START.minusDays(92)));
        sender.sendDueMessages();
        clock.set(START.minusDays(31));
        inTransaction(() -> {
            outbox.queueSms("+91ok", "x", "sent 31 days ago", null);
            outbox.queueSms("+91fail", "x", "failed 31 days ago", START.minusDays(32));
        });
        sender.sendDueMessages();
        when(emailService.isEnabled()).thenReturn(false);
        inTransaction(() -> outbox.queueEmail("asha@example.com", "s", "<p>x</p>", "skipped 31 days ago", null));
        sender.sendDueMessages();
        clock.set(START.minusDays(1));
        inTransaction(() -> outbox.queueSms("+91ok", "x", "sent yesterday", null));
        sender.sendDueMessages();
        clock.set(START.minusDays(31)); // queued long ago and never sent (e.g. the sender was off)
        inTransaction(() -> outbox.queueSms("+91later", "x", "never sent, 31 days old", null));
        clock.set(START);

        assertThat(sender.deleteOldMessages()).isEqualTo(3);

        assertThat(messages()).extracting(OutboxMessage::getDescription).containsExactlyInAnyOrder(
                "failed 31 days ago", "never sent, 31 days old", "sent yesterday");
    }

    // ------------------------------------------------------------------ helpers

    private void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    private List<OutboxMessage> messages() {
        return repository.findAll().stream().sorted(Comparator.comparing(OutboxMessage::getId)).toList();
    }

    private OutboxMessage only() {
        List<OutboxMessage> all = messages();
        assertThat(all).hasSize(1);
        return all.get(0);
    }

    /** A clock the test moves by hand. */
    static class MutableClock extends Clock {
        private Instant now = Instant.now();

        void set(LocalDateTime time) {
            now = time.atZone(ZoneId.systemDefault()).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @TestConfiguration
    static class TestClock {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }
}
