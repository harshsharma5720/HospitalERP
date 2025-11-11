package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Receptionist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceptionistRepository extends JpaRepository<Receptionist,Long> {

    Optional<Receptionist> findByUserName(String userName);
    Optional<Receptionist> findByUser_Id(Long userId);
}
