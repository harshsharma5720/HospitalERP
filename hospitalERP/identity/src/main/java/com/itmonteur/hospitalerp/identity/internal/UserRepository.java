package com.itmonteur.hospitalerp.identity.internal;

import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // ---------- admin dashboard (docs/ADMIN_DASHBOARD_PLAN.md) ----------

    /** Rows of [role, count] for accounts that aren't deactivated. */
    @Query("SELECT u.role, COUNT(u) FROM User u WHERE u.active = true GROUP BY u.role")
    List<Object[]> countActivePerRole();

    /** Creation times in [from, to); accounts older than users.created_at have none and never match. */
    @Query("SELECT u.createdAt FROM User u WHERE u.role = :role AND u.createdAt >= :from AND u.createdAt < :to")
    List<LocalDateTime> findCreationTimes(@Param("role") Role role, @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    @Query("SELECT MIN(u.createdAt) FROM User u")
    Optional<LocalDateTime> findFirstCreationTime();
}
