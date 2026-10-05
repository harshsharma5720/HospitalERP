package com.itmonteur.hospitalerp.audit.internal;

import com.itmonteur.hospitalerp.audit.AuditAction;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * One row of the append-only audit log (table {@code audit_log}, docs/AUDIT_LOG_PLAN.md). Never changed
 * after it is written; no foreign keys, so it outlives deleted accounts. The actor's username and role are
 * copied in as they were at that moment.
 */
@Entity
@Immutable
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    private Long actorUserId;

    private String actorUsername;

    @Column(length = 20)
    private String actorRole;

    // varchar, not a MySQL enum: a new action needs no migration
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    private Long patientId;

    private Long targetId;

    private String details;

    @Column(length = 45)
    private String ipAddress;

    protected AuditEntry() {
        // for JPA
    }

    public AuditEntry(LocalDateTime occurredAt, Long actorUserId, String actorUsername, String actorRole,
                      AuditAction action, Long patientId, Long targetId, String details, String ipAddress) {
        this.occurredAt = occurredAt;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.patientId = patientId;
        this.targetId = targetId;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getActorRole() {
        return actorRole;
    }

    public AuditAction getAction() {
        return action;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDetails() {
        return details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public String toString() {
        return "AuditEntry{" + action + ", actor=" + actorUsername + ", patient=" + patientId + ", target=" + targetId
                + ", at=" + occurredAt + "}";
    }
}
