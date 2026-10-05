package com.itmonteur.hospitalerp.audit.internal;

import com.itmonteur.hospitalerp.audit.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/** Append-only on purpose: a plain {@link Repository} with save and search, no update or delete methods. */
public interface AuditEntryRepository extends Repository<AuditEntry, Long> {

    AuditEntry save(AuditEntry entry);

    /** Every filter is optional (null); {@code to} is exclusive. */
    @Query("""
            SELECT e FROM AuditEntry e
            WHERE (:patientId IS NULL OR e.patientId = :patientId)
              AND (:username IS NULL OR e.actorUsername = :username)
              AND (:action IS NULL OR e.action = :action)
              AND (:from IS NULL OR e.occurredAt >= :from)
              AND (:to IS NULL OR e.occurredAt < :to)""")
    Page<AuditEntry> search(@Param("patientId") Long patientId, @Param("username") String username,
                            @Param("action") AuditAction action, @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to, Pageable pageable);
}
