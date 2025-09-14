package ITmonteur.example.hospitalERP.repositories;
import ITmonteur.example.hospitalERP.entities.PtInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface PtInfoRepository extends JpaRepository<PtInfo, Long> {


}
