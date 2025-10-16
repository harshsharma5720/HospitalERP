package ITmonteur.example.hospitalERP.repositories;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.PtInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface PtInfoRepository extends JpaRepository<PtInfo, Long> {

    Optional<PtInfo> findByUserName(String userName);


}
