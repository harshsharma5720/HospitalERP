package com.itmonteur.hospitalerp.staff.internal;

import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor , Long> {

    Optional<Doctor> findByEmail(String email);
    Optional<Doctor> findByName(String name);
    Optional<Doctor> findByUserName(String userName);
    Optional<Doctor> findByUserId(Long userId);
    Optional<List<Doctor>> findBySpecialist(Specialist specialist);
}
