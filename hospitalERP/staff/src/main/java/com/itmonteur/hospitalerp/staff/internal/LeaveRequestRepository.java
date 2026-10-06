package com.itmonteur.hospitalerp.staff.internal;

import com.itmonteur.hospitalerp.staff.LeaveRequest;
import com.itmonteur.hospitalerp.staff.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByStatus(LeaveStatus status);
    List<LeaveRequest> findByUserId(Long userId);
    List<LeaveRequest> findByUserIdAndStatus(Long userId, LeaveStatus status);
    List<LeaveRequest> findAllByStatus(LeaveStatus status);

    // Overlap check against PENDING/APPROVED leaves (rejected leaves can be re-applied for)
    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l WHERE l.user.id = :userId "
            + "AND l.status <> com.itmonteur.hospitalerp.staff.LeaveStatus.REJECTED "
            + "AND l.startDate <= :endDate AND l.endDate >= :startDate "
            + "AND (:excludeId IS NULL OR l.id <> :excludeId)")
    boolean existsOverlapping(@Param("userId") Long userId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("excludeId") Long excludeId);

    // True when the user has an APPROVED leave covering the given date
    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l WHERE l.user.id = :userId "
            + "AND l.status = com.itmonteur.hospitalerp.staff.LeaveStatus.APPROVED "
            + "AND l.startDate <= :date AND l.endDate >= :date")
    boolean isOnApprovedLeave(@Param("userId") Long userId, @Param("date") LocalDate date);

    // Admin dashboard: active doctors with an APPROVED leave covering the date (each doctor once)
    @Query("SELECT COUNT(DISTINCT l.user.id) FROM LeaveRequest l "
            + "WHERE l.status = com.itmonteur.hospitalerp.staff.LeaveStatus.APPROVED "
            + "AND l.startDate <= :date AND l.endDate >= :date "
            + "AND l.user.role = com.itmonteur.hospitalerp.identity.Role.DOCTOR AND l.user.active = true")
    long countDoctorsOnApprovedLeave(@Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM LeaveRequest l WHERE l.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
