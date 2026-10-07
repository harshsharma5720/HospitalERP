package com.itmonteur.hospitalerp.notifications.internal;

import com.itmonteur.hospitalerp.notifications.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    /** Due messages: pending ones whose time has come, and claimed ones whose sender died (lease over). */
    @Query("SELECT m.id FROM OutboxMessage m WHERE m.status IN ("
            + "com.itmonteur.hospitalerp.notifications.OutboxStatus.PENDING, "
            + "com.itmonteur.hospitalerp.notifications.OutboxStatus.SENDING) "
            + "AND m.nextAttemptAt <= :now ORDER BY m.nextAttemptAt, m.id")
    List<Long> findDueIds(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Claims a due message for one sender until {@code leaseUntil}. Returns 1 when this caller got it, 0 when
     * another sender was faster - the condition is re-checked inside the UPDATE.
     */
    @Modifying
    @Query("UPDATE OutboxMessage m SET m.status = com.itmonteur.hospitalerp.notifications.OutboxStatus.SENDING, "
            + "m.nextAttemptAt = :leaseUntil WHERE m.id = :id AND m.status IN ("
            + "com.itmonteur.hospitalerp.notifications.OutboxStatus.PENDING, "
            + "com.itmonteur.hospitalerp.notifications.OutboxStatus.SENDING) AND m.nextAttemptAt <= :now")
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now, @Param("leaseUntil") LocalDateTime leaseUntil);

    @Modifying
    @Query("DELETE FROM OutboxMessage m WHERE m.status IN :statuses AND m.createdAt < :before")
    int deleteCreatedBefore(@Param("statuses") Collection<OutboxStatus> statuses, @Param("before") LocalDateTime before);
}
