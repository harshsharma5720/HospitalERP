package com.itmonteur.hospitalerp.billing.internal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {

    Optional<Bill> findByAppointmentId(Long appointmentId);

    // Locked while it changes: the desk and an online payment may change the same bill at the same time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bill b WHERE b.id = :id")
    Optional<Bill> findByIdForUpdate(@Param("id") Long id);

    // A patient's bills with their lines in one query (MyBillService)
    @EntityGraph(attributePaths = "items")
    List<Bill> findByPatientIdAndStatusNotOrderByVisitDateDescIdDesc(Long patientId, BillStatus status);

    Optional<Bill> findByIdAndPatientIdAndStatusNot(Long id, Long patientId, BillStatus status);
}
