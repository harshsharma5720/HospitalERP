package com.itmonteur.hospitalerp.notifications.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * When the outbox sender runs: right after a transaction that queued messages commits (in the background, so
 * the request doesn't wait), every minute for retries, and nightly for the clean-up.
 * {@code app.notifications.sender.enabled=false} switches it off (tests drive {@link OutboxSender} themselves).
 */
@Component
@ConditionalOnProperty(name = "app.notifications.sender.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OutboxScheduler.class);

    private final OutboxSender sender;

    public OutboxScheduler(OutboxSender sender) {
        this.sender = sender;
    }

    @Async
    public void sendSoon() {
        sendDue();
    }

    @Scheduled(fixedDelayString = "${app.notifications.send-interval-ms:60000}", initialDelayString = "30000")
    public void sendDue() {
        try {
            sender.sendDueMessages();
        } catch (RuntimeException e) {
            logger.error("Outbox sender run failed; it runs again later", e);
        }
    }

    @Scheduled(cron = "${app.notifications.cleanup-cron:0 30 3 * * *}")
    public void deleteOldMessages() {
        try {
            int deleted = sender.deleteOldMessages();
            if (deleted > 0) {
                logger.info("Deleted {} old outbox messages", deleted);
            }
        } catch (RuntimeException e) {
            logger.error("Outbox clean-up failed; it runs again tomorrow", e);
        }
    }
}
