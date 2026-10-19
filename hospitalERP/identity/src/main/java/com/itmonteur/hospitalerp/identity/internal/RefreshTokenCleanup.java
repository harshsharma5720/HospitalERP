package com.itmonteur.hospitalerp.identity.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Once a night: the tokens of logins that ended or expired over 30 days ago are deleted (docs/SAFER_LOGIN_TOKENS_PLAN.md). */
@Component
public class RefreshTokenCleanup {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanup.class);

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenCleanup(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Scheduled(cron = "${app.auth.cleanup-cron:0 45 3 * * *}")
    public void deleteOldTokens() {
        try {
            int deleted = refreshTokenService.deleteOld();
            if (deleted > 0) {
                logger.info("Deleted {} old refresh tokens", deleted);
            }
        } catch (RuntimeException e) {
            logger.error("Refresh token clean-up failed; it runs again tomorrow", e);
        }
    }
}
