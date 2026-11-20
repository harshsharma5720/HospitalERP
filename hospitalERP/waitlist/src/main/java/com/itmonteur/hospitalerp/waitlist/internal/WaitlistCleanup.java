package com.itmonteur.hospitalerp.waitlist.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Once a night: waits are deleted 30 days after their day (docs/WAITLIST_PLAN.md). */
@Component
public class WaitlistCleanup {

    private static final Logger logger = LoggerFactory.getLogger(WaitlistCleanup.class);

    private final WaitlistService waitlistService;

    public WaitlistCleanup(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @Scheduled(cron = "${app.waitlist.cleanup-cron:0 20 4 * * *}")
    public void deleteOldWaits() {
        try {
            int deleted = waitlistService.deleteOld();
            if (deleted > 0) {
                logger.info("Deleted {} old waits", deleted);
            }
        } catch (RuntimeException e) {
            logger.error("Waitlist clean-up failed; it runs again tomorrow", e);
        }
    }
}
