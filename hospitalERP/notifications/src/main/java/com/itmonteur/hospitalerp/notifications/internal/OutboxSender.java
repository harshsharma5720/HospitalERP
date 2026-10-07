package com.itmonteur.hospitalerp.notifications.internal;

import com.itmonteur.hospitalerp.notifications.EmailService;
import com.itmonteur.hospitalerp.notifications.OutboxStatus;
import com.itmonteur.hospitalerp.notifications.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Delivers due outbox messages (docs/RELIABLE_NOTIFICATIONS_PLAN.md). Each message is first claimed - so two
 * senders never deliver the same one - then sent outside any transaction, then its result is stored. A sender
 * that dies mid-way leaves the claim to run out after {@link #LEASE}; the message is then due again.
 */
@Component
public class OutboxSender {

    private static final Logger logger = LoggerFactory.getLogger(OutboxSender.class);

    static final Duration LEASE = Duration.ofMinutes(10);
    static final int BATCH_SIZE = 50;
    static final Duration KEEP_DONE = Duration.ofDays(30);
    static final Duration KEEP_FAILED = Duration.ofDays(90);

    private final OutboxRepository repository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public OutboxSender(OutboxRepository repository, EmailService emailService, SmsService smsService,
                        PlatformTransactionManager transactionManager, Clock clock) {
        this.repository = repository;
        this.emailService = emailService;
        this.smsService = smsService;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    /** One attempt for every due message (at most one batch). Returns how many were attempted. */
    public int sendDueMessages() {
        List<Long> due = transaction.execute(status ->
                repository.findDueIds(now(), PageRequest.of(0, BATCH_SIZE)));
        int attempted = 0;
        for (Long id : due) {
            if (claim(id)) {
                attempt(id);
                attempted++;
            }
        }
        return attempted;
    }

    /** Deletes delivered and skipped messages after 30 days, failed ones after 90 days. */
    public int deleteOldMessages() {
        LocalDateTime now = now();
        Integer deleted = transaction.execute(status ->
                repository.deleteCreatedBefore(EnumSet.of(OutboxStatus.SENT, OutboxStatus.SKIPPED), now.minus(KEEP_DONE))
                        + repository.deleteCreatedBefore(EnumSet.of(OutboxStatus.FAILED), now.minus(KEEP_FAILED)));
        return deleted == null ? 0 : deleted;
    }

    private boolean claim(Long id) {
        LocalDateTime now = now();
        Integer claimed = transaction.execute(status -> repository.claim(id, now, now.plus(LEASE)));
        return claimed != null && claimed == 1;
    }

    private void attempt(Long id) {
        OutboxMessage message = transaction.execute(status -> repository.findById(id).orElse(null));
        if (message == null) {
            return;
        }
        String skipReason = null;
        String error = null;
        try {
            skipReason = deliver(message);
        } catch (Exception e) {
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
            logger.warn("Could not send {} (outbox message {}, attempt {}): {}", message.getDescription(), id,
                    message.getAttempts() + 1, error);
        }
        String skipped = skipReason;
        String failure = error;
        transaction.executeWithoutResult(status -> repository.findById(id).ifPresent(stored -> {
            if (failure != null) {
                stored.markFailedAttempt(failure, now());
            } else if (skipped != null) {
                stored.markSkipped(skipped);
            } else {
                stored.markSent(now());
            }
        }));
    }

    /** Sends the message; returns a reason when its channel isn't configured (nothing was sent). */
    private String deliver(OutboxMessage message) throws Exception {
        switch (message.getChannel()) {
            case EMAIL -> {
                if (!emailService.isEnabled()) {
                    return "Email is not configured (no SMTP host)";
                }
                emailService.sendHtml(message.getRecipient(), message.getSubject(), message.getBody());
            }
            case SMS -> {
                if (!smsService.isEnabled()) {
                    return "SMS is not configured (no Twilio account)";
                }
                smsService.send(message.getRecipient(), message.getBody());
            }
        }
        return null;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
