package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentStatus;
import com.itmonteur.hospitalerp.appointments.DoctorAppointmentCount;
import com.itmonteur.hospitalerp.appointments.SpecializationCount;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.scheduling.Shift;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/*
 * "Pending" = SCHEDULED/CONFIRMED and not completed; "completed" = status COMPLETED.
 * The legacy isCompleted flag is also checked so rows written before status was
 * kept in sync are still classified correctly. Cancelled appointments are in neither list.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    String PENDING = " a.isCompleted = false AND a.status IN ("
            + "com.itmonteur.hospitalerp.appointments.AppointmentStatus.SCHEDULED, "
            + "com.itmonteur.hospitalerp.appointments.AppointmentStatus.CONFIRMED) ";
    String COMPLETED = " (a.isCompleted = true OR a.status = "
            + "com.itmonteur.hospitalerp.appointments.AppointmentStatus.COMPLETED) ";

    List<Appointment> findByDoctor_Name(String doctorName);

    List<Appointment> findByPtInfo_PatientId(Long patientId);

    @Query("SELECT a FROM Appointment a WHERE a.doctor = :doctor AND a.shift = :shift")
    List<Appointment> findByDoctorAndShift(@Param("doctor") Doctor doctor,
                                           @Param("shift") Shift shift);
    List<Appointment> findByDoctor_IdAndDateBetween(Long doctorId, LocalDate start, LocalDate end);
    List<Appointment> findByDoctor_Id(Long doctorId);

    @Query("SELECT a FROM Appointment a WHERE a.ptInfo.patientId = :patientId AND" + PENDING + "ORDER BY a.date")
    List<Appointment> findPendingByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT a FROM Appointment a WHERE a.ptInfo.patientId = :patientId AND" + COMPLETED + "ORDER BY a.date DESC")
    List<Appointment> findCompletedByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT a FROM Appointment a WHERE" + PENDING + "ORDER BY a.date")
    List<Appointment> findAllPending();

    @Query("SELECT a FROM Appointment a WHERE" + COMPLETED + "ORDER BY a.date DESC")
    List<Appointment> findAllCompleted();

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND" + PENDING + "ORDER BY a.date")
    List<Appointment> findPendingByDoctorId(@Param("doctorId") Long doctorId);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND" + COMPLETED + "ORDER BY a.date DESC")
    List<Appointment> findCompletedByDoctorId(@Param("doctorId") Long doctorId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND" + PENDING)
    long countPendingByDoctorId(@Param("doctorId") Long doctorId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND" + COMPLETED)
    long countCompletedByDoctorId(@Param("doctorId") Long doctorId);

    long countByDoctorUserIdAndStatus(Long userId, AppointmentStatus status);

    // Ids of the doctor's slots (from the given date on) that any appointment refers to, whatever its status
    @Query("SELECT DISTINCT a.slot.id FROM Appointment a WHERE a.slot.doctor.id = :doctorId AND a.slot.date >= :fromDate")
    List<Long> findSlotIdsInUse(@Param("doctorId") Long doctorId, @Param("fromDate") LocalDate fromDate);

    // Whether the doctor has ever seen (or is booked with) this patient
    boolean existsByDoctor_IdAndPtInfo_PatientId(Long doctorId, Long patientId);

    // Any appointment at all, whatever its status (an account with history may only be deactivated)
    boolean existsByPtInfo_PatientId(Long patientId);

    boolean existsByDoctor_Id(Long doctorId);

    // Upcoming appointments on the given date that have not been reminded yet
    @Query("SELECT a FROM Appointment a WHERE a.date = :date AND" + PENDING
            + "AND (a.reminderSent IS NULL OR a.reminderSent = false)")
    List<Appointment> findDueForReminder(@Param("date") LocalDate date);

    // Used when deleting a relative: keep the appointment history, drop the link
    @Modifying
    @Query("UPDATE Appointment a SET a.relative = null WHERE a.relative.id = :relativeId")
    void clearRelative(@Param("relativeId") Long relativeId);

    @Modifying
    @Query("DELETE FROM Appointment a WHERE a.ptInfo.patientId = :patientId")
    void deleteByPatientId(@Param("patientId") Long patientId);

    @Modifying
    @Query("DELETE FROM Appointment a WHERE a.doctor.id = :doctorId")
    void deleteByDoctorId(@Param("doctorId") Long doctorId);

    // ------------------------------------------------------------------ admin dashboard (AppointmentStatistics)

    String NOT_CANCELLED = " a.status NOT IN ("
            + "com.itmonteur.hospitalerp.appointments.AppointmentStatus.CANCELLED_BY_DOCTOR, "
            + "com.itmonteur.hospitalerp.appointments.AppointmentStatus.CANCELLED_BY_PATIENT) ";

    /** Rows of [date, status, isCompleted, count]; AppointmentStatistics turns them into outcomes. */
    @Query("SELECT a.date, a.status, a.isCompleted, COUNT(a) FROM Appointment a "
            + "WHERE a.date BETWEEN :from AND :to GROUP BY a.date, a.status, a.isCompleted")
    List<Object[]> countPerDayStatusAndCompletedFlag(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT new com.itmonteur.hospitalerp.appointments.SpecializationCount(d.specialist, COUNT(a)) "
            + "FROM Appointment a JOIN a.doctor d WHERE a.date BETWEEN :from AND :to AND" + NOT_CANCELLED
            + "GROUP BY d.specialist ORDER BY COUNT(a) DESC, d.specialist")
    List<SpecializationCount> countPerSpecialization(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                                     Pageable pageable);

    @Query("SELECT new com.itmonteur.hospitalerp.appointments.DoctorAppointmentCount(d.id, d.name, d.specialist, "
            + "COUNT(a), SUM(CASE WHEN" + COMPLETED + "THEN 1 ELSE 0 END)) "
            + "FROM Appointment a JOIN a.doctor d WHERE a.date BETWEEN :from AND :to AND" + NOT_CANCELLED
            + "GROUP BY d.id, d.name, d.specialist ORDER BY COUNT(a) DESC, d.name")
    List<DoctorAppointmentCount> countPerDoctor(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                                Pageable pageable);
}
