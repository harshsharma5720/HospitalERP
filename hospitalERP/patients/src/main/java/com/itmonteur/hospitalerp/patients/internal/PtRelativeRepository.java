package com.itmonteur.hospitalerp.patients.internal;

import com.itmonteur.hospitalerp.patients.PtRelative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PtRelativeRepository extends JpaRepository<PtRelative,Long> {

    List<PtRelative> findByPtInfoPatientId(Long patientId);
}
