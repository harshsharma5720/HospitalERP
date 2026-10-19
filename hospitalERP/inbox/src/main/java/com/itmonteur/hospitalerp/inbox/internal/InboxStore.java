package com.itmonteur.hospitalerp.inbox.internal;

import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.inbox.InboxKind;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The notification centre's messages (docs/NOTIFICATION_CENTRE_PLAN.md): adding them, a user's newest ones, the number
 * unread, marking them read, and deleting old ones. Only the last {@link #KEEP} count; the nightly clean-up deletes the
 * rest.
 */
@Service
public class InboxStore {

    static final Duration KEEP = Duration.ofDays(90);
    public static final int NEWEST = 50;
    static final int MAX_TITLE = 120;
    static final int MAX_BODY = 500;
    static final int MAX_LINK = 200;

    private final InboxMessageRepository messages;
    private final Clock clock;

    public InboxStore(InboxMessageRepository messages, Clock clock) {
        this.messages = messages;
        this.clock = clock;
    }

    @Transactional
    public void add(long userId, InboxKind kind, String title, String body, String link) {
        messages.save(new InboxMessage(userId, kind, cut(title, MAX_TITLE), cut(body, MAX_BODY),
                link == null || link.length() > MAX_LINK ? null : link, now()));
    }

    /** The user's newest {@link #NEWEST} messages and how many of the kept ones are unread. */
    @Transactional(readOnly = true)
    public InboxDTO newest(long userId) {
        LocalDateTime since = now().minus(KEEP);
        List<InboxMessageDTO> newest = messages
                .findByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDescIdDesc(userId, since, PageRequest.of(0, NEWEST))
                .stream().map(InboxMessageDTO::of).toList();
        return new InboxDTO(newest, unread(userId));
    }

    @Transactional(readOnly = true)
    public long unread(long userId) {
        return messages.countByUserIdAndReadAtIsNullAndCreatedAtGreaterThanEqual(userId, now().minus(KEEP));
    }

    /** One of the user's own messages read; another user's is "not found". */
    @Transactional
    public InboxMessageDTO markRead(long userId, long messageId) {
        InboxMessage message = messages.findByIdAndUserId(messageId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));
        message.markRead(now());
        return InboxMessageDTO.of(message);
    }

    /** Every unread message of the user read; how many. */
    @Transactional
    public int markAllRead(long userId) {
        return messages.markAllRead(userId, now());
    }

    /** Deletes the messages older than {@link #KEEP}; how many. */
    @Transactional
    public int deleteOld() {
        return messages.deleteCreatedBefore(now().minus(KEEP));
    }

    static String cut(String text, int max) {
        String value = text == null ? "" : text.strip();
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
