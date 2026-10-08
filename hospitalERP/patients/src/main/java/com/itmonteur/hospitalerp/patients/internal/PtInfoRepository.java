package com.itmonteur.hospitalerp.patients.internal;
import com.itmonteur.hospitalerp.patients.PtInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface PtInfoRepository extends JpaRepository<PtInfo, Long> {

    Optional<PtInfo> findByUserName(String userName);
    Optional<PtInfo> findByUser_Id(Long id);

    // Front desk (docs/WALK_IN_REGISTRATION_PLAN.md): a cheap first filter; PtInfoService compares the digits exactly
    List<PtInfo> findByContactNoEndingWith(String suffix);

    // Walk-in records (no login account), for the admin dashboard
    long countByUserIsNull();

    @Query("SELECT p.createdAt FROM PtInfo p WHERE p.user IS NULL AND p.createdAt >= :from AND p.createdAt < :to")
    List<LocalDateTime> findWalkInCreationTimes(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT MIN(p.createdAt) FROM PtInfo p WHERE p.user IS NULL")
    Optional<LocalDateTime> findFirstWalkInCreationTime();

}
