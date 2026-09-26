package com.itmonteur.hospitalerp.scheduling.internal;

import com.itmonteur.hospitalerp.scheduling.Slot;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.scheduling.Shift;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
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

    // A doctor's slots from the given date on (used when no slot has to be kept)
    @Modifying
    @Query("DELETE FROM Slot s WHERE s.doctor.id = :doctorId AND s.date >= :fromDate")
    void deleteFromDate(@Param("doctorId") Long doctorId, @Param("fromDate") LocalDate fromDate);

    // Same, but keeps the given slots (keepIds must not be empty)
    @Modifying
    @Query("DELETE FROM Slot s WHERE s.doctor.id = :doctorId AND s.date >= :fromDate AND s.id NOT IN :keepIds")
    void deleteFromDateExcept(@Param("doctorId") Long doctorId, @Param("fromDate") LocalDate fromDate,
                              @Param("keepIds") Collection<Long> keepIds);
}
