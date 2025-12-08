package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.Slot;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot,Long> {

    List<Slot> findByDoctorAndDateAndShiftAndAvailableTrue(Doctor doctor, LocalDate date, Shift shift);

    List<Slot> findByDoctorAndDateAndShift(Doctor doctor, LocalDate date, Shift shift);
    List<Slot> findByDoctor_IdAndDateBetween(Long doctorId, LocalDate start, LocalDate end);


}
