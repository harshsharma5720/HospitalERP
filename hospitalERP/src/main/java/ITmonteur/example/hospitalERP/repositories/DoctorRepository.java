package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor , Long> {

    Optional<Doctor> findByEmail(String email);
    Optional<Doctor> findByName(String name);
    Optional<Doctor> findByUserId(Long userId);
}
