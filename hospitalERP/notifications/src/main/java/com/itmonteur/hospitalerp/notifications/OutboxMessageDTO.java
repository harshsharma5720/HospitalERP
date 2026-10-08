package com.itmonteur.hospitalerp.notifications;

import java.time.LocalDateTime;

/** An outbox message as the admin sees it - without the message text itself. */
public record OutboxMessageDTO(Long id, OutboxChannel channel, String recipient, String subject, String description,
                               OutboxStatus status, int attempts, String lastError, LocalDateTime createdAt,
                               LocalDateTime nextAttemptAt, LocalDateTime giveUpAt, LocalDateTime sentAt) {
}
