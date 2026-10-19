package com.itmonteur.hospitalerp.identity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * One refresh token of a login session (docs/SAFER_LOGIN_TOKENS_PLAN.md), kept as its SHA-256 hash. It is replaced
 * when used ({@code replacedAt}); the session's rows share a {@code sessionId} and end together ({@code endedAt}).
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 36)
    private String sessionId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Until when this token can be used (patients: 7 days after it was made; staff: the session's end). */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** The login's latest end, whatever the refreshes (patients: 90 days; staff: 12 hours). */
    @Column(nullable = false)
    private LocalDateTime sessionExpiresAt;

    private LocalDateTime replacedAt;

    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private SessionEndReason endReason;

    protected RefreshToken() {
    }

    RefreshToken(Long userId, String sessionId, String tokenHash, LocalDateTime createdAt, LocalDateTime expiresAt,
                 LocalDateTime sessionExpiresAt) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.sessionExpiresAt = sessionExpiresAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getSessionExpiresAt() { return sessionExpiresAt; }
    public LocalDateTime getReplacedAt() { return replacedAt; }
    public void setReplacedAt(LocalDateTime replacedAt) { this.replacedAt = replacedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public SessionEndReason getEndReason() { return endReason; }

    void end(LocalDateTime when, SessionEndReason reason) {
        this.endedAt = when;
        this.endReason = reason;
    }
}
