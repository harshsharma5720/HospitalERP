package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentStatus;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.scheduling.Shift;
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
}
