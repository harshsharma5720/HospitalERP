package com.itmonteur.hospitalerp.inbox.internal;

import java.util.List;

/** A user's newest messages, newest first, and how many of their messages are unread. */
public record InboxDTO(List<InboxMessageDTO> messages, long unread) {
}
