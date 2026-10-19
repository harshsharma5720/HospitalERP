package com.itmonteur.hospitalerp.identity.internal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Locked: two tabs may refresh with the same token at the same time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RefreshToken t WHERE t.tokenHash = :hash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("hash") String hash);

    Optional<RefreshToken> findByTokenHash(String hash);

    List<RefreshToken> findBySessionIdAndEndedAtIsNull(String sessionId);

    List<RefreshToken> findByUserIdAndEndedAtIsNull(Long userId);

    // A session's end is never before its tokens' expiry, so this also covers sessions that ran out
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :before OR t.endedAt < :before")
    int deleteExpiredOrEndedBefore(@Param("before") LocalDateTime before);
}
