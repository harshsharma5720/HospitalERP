package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.LeaveRequest;
import ITmonteur.example.hospitalERP.entities.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByStatus(LeaveStatus status);
    List<LeaveRequest> findByUserId(Long userId);
    List<LeaveRequest> findByUserIdAndStatus(Long userId, LeaveStatus status);
    List<LeaveRequest> findAllByStatus(LeaveStatus status);
    boolean existsByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long userId, LocalDate start, LocalDate end);
}
