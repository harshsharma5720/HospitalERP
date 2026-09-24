package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.Slot;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Shift;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<Slot,Long> {

    List<Slot> findByDoctorAndDateAndShiftAndAvailableTrue(Doctor doctor, LocalDate date, Shift shift);

    List<Slot> findByDoctorAndDateAndShift(Doctor doctor, LocalDate date, Shift shift);
    List<Slot> findByDoctor_IdAndDateBetween(Long doctorId, LocalDate start, LocalDate end);

    // Row lock (SELECT ... FOR UPDATE) so two concurrent bookings cannot take the same slot
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :id")
    Optional<Slot> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM Slot s WHERE s.doctor.id = :doctorId")
    void deleteByDoctorId(@Param("doctorId") Long doctorId);

    // Slots from the given date on that no appointment (active or past) refers to
    @Modifying
    @Query("DELETE FROM Slot s WHERE s.doctor.id = :doctorId AND s.date >= :fromDate "
            + "AND NOT EXISTS (SELECT a FROM Appointment a WHERE a.slot = s)")
    void deleteUnusedFromDate(@Param("doctorId") Long doctorId, @Param("fromDate") LocalDate fromDate);
}
