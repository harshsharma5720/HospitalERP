package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Native query to fetch appointments by doctor name
    @Query(value = "SELECT * FROM appointment a WHERE a.doctor = :doctorName", nativeQuery = true)
    List<Appointment> findAppointmentsByDoctor(@Param("doctorName") String doctorName);

    List<Appointment> findByPtInfo_PatientId(Long patientId);

    @Query("SELECT a FROM Appointment a WHERE a.doctor = :doctor AND a.shift = :shift")
    List<Appointment> findByDoctorAndShift(@Param("doctor") Doctor doctor,
                                           @Param("shift") Shift shift);

    List<Appointment> findByDoctor_Id(Long doctorId);
    List<Appointment> findByPtInfo_PatientIdAndIsCompletedTrue(Long patientId);
    List<Appointment> findByPtInfo_PatientIdAndIsCompletedFalse(Long patientId);

    List<Appointment> findAllByIsCompletedFalse();
    List<Appointment> findAllByIsCompletedTrue();

    List<Appointment> findByDoctor_IdAndIsCompletedFalse(Long doctorId);
    List<Appointment> findByDoctor_IdAndIsCompletedTrue(Long doctorId);
}
