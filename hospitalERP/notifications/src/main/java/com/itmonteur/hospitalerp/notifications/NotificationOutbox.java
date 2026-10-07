package com.itmonteur.hospitalerp.notifications;

import com.itmonteur.hospitalerp.common.ConflictException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.notifications.internal.OutboxMessage;
import com.itmonteur.hospitalerp.notifications.internal.OutboxRepository;
import com.itmonteur.hospitalerp.notifications.internal.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // ------------------------------------------------------------------ admin (docs/RELIABLE_NOTIFICATIONS_PLAN.md, C.3)

    /** Messages with the given status (all when null), newest first; {@code size} is capped at 100. */
    @Transactional(readOnly = true)
    public Page<OutboxMessageDTO> search(OutboxStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<OutboxMessage> result = status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        return result.map(NotificationOutbox::toDTO);
    }

    /**
     * Sends a failed (or skipped) message again: due at once, retried for another 24 hours, sent right after
     * this commit. Anything else is a conflict - it is still being sent, or already was.
     */
    @Transactional
    public OutboxMessageDTO resend(Long id) {
        OutboxMessage message = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        if (message.getStatus() != OutboxStatus.FAILED && message.getStatus() != OutboxStatus.SKIPPED) {
            throw new ConflictException("Only failed or skipped messages can be resent; this one is "
                    + message.getStatus().name().toLowerCase());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        message.resend(now, now.plus(MAX_RETRY_WINDOW));
        logger.info("Outbox message {} ({}) queued again by an admin", id, message.getDescription());
        sendAfterCommit();
        return toDTO(message);
    }

    /** Messages that could not be delivered (failed, not resent yet). */
    @Transactional(readOnly = true)
    public long countUndelivered() {
        return repository.countByStatus(OutboxStatus.FAILED);
    }

    private static OutboxMessageDTO toDTO(OutboxMessage m) {
        return new OutboxMessageDTO(m.getId(), m.getChannel(), m.getRecipient(), m.getSubject(), m.getDescription(),
                m.getStatus(), m.getAttempts(), m.getLastError(), m.getCreatedAt(), m.getNextAttemptAt(),
                m.getGiveUpAt(), m.getSentAt());
    }

    // ------------------------------------------------------------------ queueing

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
