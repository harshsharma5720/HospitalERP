package com.itmonteur.hospitalerp.billing.internal;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {

    Optional<Bill> findByAppointmentId(Long appointmentId);

    // A patient's bills with their lines in one query (MyBillService)
    @EntityGraph(attributePaths = "items")
    List<Bill> findByPatientIdAndStatusNotOrderByVisitDateDescIdDesc(Long patientId, BillStatus status);

    Optional<Bill> findByIdAndPatientIdAndStatusNot(Long id, Long patientId, BillStatus status);
}
