package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.Receptionist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceptionistRepository extends JpaRepository<Receptionist,Long> {

}
