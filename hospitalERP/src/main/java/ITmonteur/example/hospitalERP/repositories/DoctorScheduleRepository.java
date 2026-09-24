package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.DoctorSchedule;
import ITmonteur.example.hospitalERP.entities.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctor_Id(Long doctorId);

    Optional<DoctorSchedule> findByDoctor_IdAndDayOfWeekAndShift(Long doctorId, DayOfWeek dayOfWeek, Shift shift);

    @Modifying
    @Query("DELETE FROM DoctorSchedule s WHERE s.doctor.id = :doctorId")
    void deleteByDoctorId(@Param("doctorId") Long doctorId);
}
