package com.itmonteur.hospitalerp.notifications.internal;

import com.itmonteur.hospitalerp.notifications.OutboxChannel;
import com.itmonteur.hospitalerp.notifications.OutboxStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One email or SMS in the outbox (table {@code notification_outbox}, docs/RELIABLE_NOTIFICATIONS_PLAN.md).
 * The text is rendered when the message is queued, so it never changes afterwards.
 */
@Entity
@Table(name = "notification_outbox")
public class OutboxMessage {

    /** Gaps between attempts: 1, 5, 15, 30, 60 minutes, then hourly. */
    static final List<Duration> RETRY_GAPS = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5),
            Duration.ofMinutes(15), Duration.ofMinutes(30), Duration.ofMinutes(60));
    static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private OutboxChannel channel;

    @Column(nullable = false)
    private String recipient;

    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false, length = 100)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(nullable = false)
    private LocalDateTime giveUpAt;

    @Column(length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    protected OutboxMessage() {
        // for JPA
    }

    /** A new message, due at once; {@code giveUpAt} only limits retries - the first attempt is always made. */
    public OutboxMessage(OutboxChannel channel, String recipient, String subject, String body, String description,
                         LocalDateTime now, LocalDateTime giveUpAt) {
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.description = description;
        this.status = OutboxStatus.PENDING;
        this.nextAttemptAt = now;
        this.giveUpAt = giveUpAt;
        this.createdAt = now;
    }

    void markSent(LocalDateTime now) {
        attempts++;
        status = OutboxStatus.SENT;
        sentAt = now;
        lastError = null;
    }

    void markSkipped(String reason) {
        status = OutboxStatus.SKIPPED;
        lastError = cut(reason);
    }

    /** A failed attempt: try again after the next gap, or give up when that would be past {@code giveUpAt}. */
    void markFailedAttempt(String error, LocalDateTime now) {
        attempts++;
        lastError = cut(error);
        LocalDateTime next = now.plus(retryGap(attempts));
        if (next.isAfter(giveUpAt)) {
            status = OutboxStatus.FAILED;
            nextAttemptAt = now;
        } else {
            status = OutboxStatus.PENDING;
            nextAttemptAt = next;
        }
    }

    /** An admin's resend: due at once, with a fresh retry window. */
    public void resend(LocalDateTime now, LocalDateTime newGiveUpAt) {
        status = OutboxStatus.PENDING;
        nextAttemptAt = now;
        giveUpAt = newGiveUpAt;
    }

    static Duration retryGap(int attempts) {
        return RETRY_GAPS.get(Math.min(attempts, RETRY_GAPS.size()) - 1);
    }

    private static String cut(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public OutboxChannel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getDescription() {
        return description;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public LocalDateTime getGiveUpAt() {
        return giveUpAt;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
