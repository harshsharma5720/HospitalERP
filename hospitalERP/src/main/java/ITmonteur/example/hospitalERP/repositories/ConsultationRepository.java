package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    Optional<Consultation> findByAppointment_AppointmentID(long appointmentId);

    // A patient's medical history, newest visit first
    @Query("SELECT c FROM Consultation c WHERE c.appointment.ptInfo.patientId = :patientId "
            + "ORDER BY c.appointment.date DESC, c.id DESC")
    List<Consultation> findHistoryByPatientId(@Param("patientId") Long patientId);

    // Bulk deletes used when an account is removed (appointments are bulk-deleted afterwards)
    @Modifying
    @Query("DELETE FROM PrescriptionItem i WHERE i.consultation.id IN "
            + "(SELECT c.id FROM Consultation c WHERE c.appointment.ptInfo.patientId = :patientId)")
    void deleteItemsByPatientId(@Param("patientId") Long patientId);

    @Modifying
    @Query("DELETE FROM Consultation c WHERE c.appointment.appointmentID IN "
            + "(SELECT a.appointmentID FROM Appointment a WHERE a.ptInfo.patientId = :patientId)")
    void deleteByPatientId(@Param("patientId") Long patientId);

    @Modifying
    @Query("DELETE FROM PrescriptionItem i WHERE i.consultation.id IN "
            + "(SELECT c.id FROM Consultation c WHERE c.appointment.doctor.id = :doctorId)")
    void deleteItemsByDoctorId(@Param("doctorId") Long doctorId);

    @Modifying
    @Query("DELETE FROM Consultation c WHERE c.appointment.appointmentID IN "
            + "(SELECT a.appointmentID FROM Appointment a WHERE a.doctor.id = :doctorId)")
    void deleteByDoctorId(@Param("doctorId") Long doctorId);
}
