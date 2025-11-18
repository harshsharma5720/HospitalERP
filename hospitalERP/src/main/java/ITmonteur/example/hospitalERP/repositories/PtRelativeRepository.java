package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.PtRelative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PtRelativeRepository extends JpaRepository<PtRelative,Long> {

    List<PtRelative> findByPtInfoPatientId(Long patientId);
}
