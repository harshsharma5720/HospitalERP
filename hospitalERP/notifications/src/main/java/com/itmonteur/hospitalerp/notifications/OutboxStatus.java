package com.itmonteur.hospitalerp.notifications;

/**
 * Where an outbox message stands (docs/RELIABLE_NOTIFICATIONS_PLAN.md).
 * PENDING: waiting for its next attempt · SENDING: a sender has claimed it · SENT: delivered ·
 * SKIPPED: its channel isn't configured (no SMTP host / Twilio account) · FAILED: still undelivered when
 * the retries ran out.
 */
public enum OutboxStatus {
    PENDING, SENDING, SENT, SKIPPED, FAILED
}
