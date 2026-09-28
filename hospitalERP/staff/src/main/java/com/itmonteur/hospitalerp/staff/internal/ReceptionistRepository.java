package com.itmonteur.hospitalerp.staff.internal;

import com.itmonteur.hospitalerp.staff.Receptionist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceptionistRepository extends JpaRepository<Receptionist,Long> {

    Optional<Receptionist> findByUserName(String userName);
    Optional<Receptionist> findByUser_Id(Long userId);
}
