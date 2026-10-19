package com.itmonteur.hospitalerp.inbox.internal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, Long> {

    // A user's newest messages since a moment
    List<InboxMessage> findByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDescIdDesc(Long userId, LocalDateTime since,
                                                                                          Pageable page);

    long countByUserIdAndReadAtIsNullAndCreatedAtGreaterThanEqual(Long userId, LocalDateTime since);

    Optional<InboxMessage> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE InboxMessage m SET m.readAt = :now WHERE m.userId = :userId AND m.readAt IS NULL")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM InboxMessage m WHERE m.createdAt < :before")
    int deleteCreatedBefore(@Param("before") LocalDateTime before);
}
