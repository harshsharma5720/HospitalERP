package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
}
