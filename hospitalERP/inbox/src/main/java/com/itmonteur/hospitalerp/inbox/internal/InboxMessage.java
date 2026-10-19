package com.itmonteur.hospitalerp.inbox.internal;

import com.itmonteur.hospitalerp.inbox.InboxKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** One message in a user's notification centre (docs/NOTIFICATION_CENTRE_PLAN.md); only its read time changes. */
@Entity
@Table(name = "inbox_messages")
public class InboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InboxKind kind;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    // A page of the app, e.g. "/appointment-details"; null when there is nothing to open
    @Column(length = 200)
    private String link;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    protected InboxMessage() {
    }

    InboxMessage(Long userId, InboxKind kind, String title, String body, String link, LocalDateTime createdAt) {
        this.userId = userId;
        this.kind = kind;
        this.title = title;
        this.body = body;
        this.link = link;
        this.createdAt = createdAt;
    }

    void markRead(LocalDateTime when) {
        if (readAt == null) {
            readAt = when;
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public InboxKind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getLink() { return link; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
}
