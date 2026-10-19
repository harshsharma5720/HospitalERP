package com.itmonteur.hospitalerp.inbox.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Once a night: messages older than 90 days are deleted (docs/NOTIFICATION_CENTRE_PLAN.md). */
@Component
public class InboxCleanup {

    private static final Logger logger = LoggerFactory.getLogger(InboxCleanup.class);

    private final InboxStore store;

    public InboxCleanup(InboxStore store) {
        this.store = store;
    }

    @Scheduled(cron = "${app.inbox.cleanup-cron:0 15 4 * * *}")
    public void deleteOldMessages() {
        try {
            int deleted = store.deleteOld();
            if (deleted > 0) {
                logger.info("Deleted {} old inbox messages", deleted);
            }
        } catch (RuntimeException e) {
            logger.error("Inbox clean-up failed; it runs again tomorrow", e);
        }
    }
}
