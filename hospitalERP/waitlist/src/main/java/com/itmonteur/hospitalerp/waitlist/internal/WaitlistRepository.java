package com.itmonteur.hospitalerp.waitlist.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    String WAITING = " com.itmonteur.hospitalerp.waitlist.internal.WaitlistStatus.WAITING ";

    // One person's wait for a doctor on a day (relativeId null: the patient themself)
    @Query("SELECT e FROM WaitlistEntry e LEFT JOIN e.relative r WHERE e.patient.patientId = :patientId"
            + " AND ((:relativeId IS NULL AND r IS NULL) OR r.id = :relativeId)"
            + " AND e.doctor.id = :doctorId AND e.date = :date AND e.status =" + WAITING)
    Optional<WaitlistEntry> findWaiting(@Param("patientId") Long patientId, @Param("relativeId") Long relativeId,
                                        @Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    // An account's waits from a day on (the patient's and their family members'), soonest first
    @Query("SELECT e FROM WaitlistEntry e WHERE e.patient.patientId = :patientId AND e.status =" + WAITING
            + "AND e.date >= :from ORDER BY e.date, e.joinedAt")
    List<WaitlistEntry> findWaitingForPatient(@Param("patientId") Long patientId, @Param("from") LocalDate from);

    // Everyone waiting for a doctor (from a day on)
    @Query("SELECT e FROM WaitlistEntry e WHERE e.doctor.id = :doctorId AND e.status =" + WAITING + "AND e.date >= :from")
    List<WaitlistEntry> findWaitingForDoctor(@Param("doctorId") Long doctorId, @Param("from") LocalDate from);

    // The front desk's list of a day: every wait, by doctor and in the order they were joined (doctorId null: all)
    @Query("SELECT e FROM WaitlistEntry e WHERE e.date = :date AND (:doctorId IS NULL OR e.doctor.id = :doctorId)"
            + " ORDER BY e.doctor.name, e.joinedAt")
    List<WaitlistEntry> findForDay(@Param("date") LocalDate date, @Param("doctorId") Long doctorId);
}
