package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.LeaveRequest;
import ITmonteur.example.hospitalERP.entities.LeaveStatus;
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
            + "AND l.status <> ITmonteur.example.hospitalERP.entities.LeaveStatus.REJECTED "
            + "AND l.startDate <= :endDate AND l.endDate >= :startDate "
            + "AND (:excludeId IS NULL OR l.id <> :excludeId)")
    boolean existsOverlapping(@Param("userId") Long userId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("excludeId") Long excludeId);

    // True when the user has an APPROVED leave covering the given date
    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l WHERE l.user.id = :userId "
            + "AND l.status = ITmonteur.example.hospitalERP.entities.LeaveStatus.APPROVED "
            + "AND l.startDate <= :date AND l.endDate >= :date")
    boolean isOnApprovedLeave(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM LeaveRequest l WHERE l.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
