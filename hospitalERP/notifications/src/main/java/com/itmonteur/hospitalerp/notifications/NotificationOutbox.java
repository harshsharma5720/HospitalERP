package com.itmonteur.hospitalerp.notifications;

import com.itmonteur.hospitalerp.notifications.internal.OutboxMessage;
import com.itmonteur.hospitalerp.notifications.internal.OutboxRepository;
import com.itmonteur.hospitalerp.notifications.internal.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Queues emails and SMS for reliable delivery (docs/RELIABLE_NOTIFICATIONS_PLAN.md). A message is stored in the
 * caller's transaction - so it exists exactly when the change it is about was committed - and sent right after
 * the commit; failures are retried with growing gaps for up to 24 hours, never after {@code notAfter}.
 */
@Service
public class NotificationOutbox {

    private static final Logger logger = LoggerFactory.getLogger(NotificationOutbox.class);

    static final Duration MAX_RETRY_WINDOW = Duration.ofHours(24);
    private static final Object SEND_AFTER_COMMIT = new Object(); // one trigger per transaction

    private final OutboxRepository repository;
    private final ObjectProvider<OutboxScheduler> scheduler;
    private final Clock clock;

    public NotificationOutbox(OutboxRepository repository, ObjectProvider<OutboxScheduler> scheduler, Clock clock) {
        this.repository = repository;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    /**
     * Queues an HTML mail ({@code html} already has the layout - see {@link EmailService#layout}).
     * {@code notAfter}: no retries after this time (e.g. the appointment), or null.
     */
    @Transactional
    public void queueEmail(String to, String subject, String html, String description, LocalDateTime notAfter) {
        queue(OutboxChannel.EMAIL, to, subject, html, description, notAfter);
    }

    /** Queues an SMS; {@code notAfter} as for {@link #queueEmail}. */
    @Transactional
    public void queueSms(String phoneNumber, String text, String description, LocalDateTime notAfter) {
        queue(OutboxChannel.SMS, phoneNumber, null, text, description, notAfter);
    }

    private void queue(OutboxChannel channel, String recipient, String subject, String body, String description,
                       LocalDateTime notAfter) {
        if (recipient == null || recipient.isBlank()) {
            logger.debug("No recipient - {} not queued", description);
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime giveUpAt = now.plus(MAX_RETRY_WINDOW);
        if (notAfter != null && notAfter.isBefore(giveUpAt)) {
            giveUpAt = notAfter;
        }
        repository.save(new OutboxMessage(channel, recipient.trim(), subject, body, description, now, giveUpAt));
        sendAfterCommit();
    }

    private void sendAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            scheduler.ifAvailable(OutboxScheduler::sendSoon);
            return;
        }
        if (TransactionSynchronizationManager.hasResource(SEND_AFTER_COMMIT)) {
            return; // this transaction already triggers a send
        }
        TransactionSynchronizationManager.bindResource(SEND_AFTER_COMMIT, Boolean.TRUE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                scheduler.ifAvailable(OutboxScheduler::sendSoon);
            }

            @Override
            public void afterCompletion(int status) {
                TransactionSynchronizationManager.unbindResourceIfPossible(SEND_AFTER_COMMIT);
            }
        });
    }
}
