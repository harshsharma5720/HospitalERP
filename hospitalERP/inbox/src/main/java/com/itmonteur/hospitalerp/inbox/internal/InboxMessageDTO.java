package com.itmonteur.hospitalerp.inbox.internal;

import com.itmonteur.hospitalerp.inbox.InboxKind;

import java.time.LocalDateTime;

/** A message as its user sees it (docs/NOTIFICATION_CENTRE_PLAN.md); {@code link}: a page of the app, or null. */
public record InboxMessageDTO(Long id, InboxKind kind, String title, String body, String link, LocalDateTime createdAt,
                              boolean read) {

    static InboxMessageDTO of(InboxMessage message) {
        return new InboxMessageDTO(message.getId(), message.getKind(), message.getTitle(), message.getBody(),
                message.getLink(), message.getCreatedAt(), message.getReadAt() != null);
    }
}
