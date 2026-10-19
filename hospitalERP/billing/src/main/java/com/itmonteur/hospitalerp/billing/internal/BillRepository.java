package com.itmonteur.hospitalerp.billing.internal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // ------------------------------------------------------------------ a doctor's earnings (DoctorEarnings)

    /** Rows of [visit date, status, number of bills, sum of discounts] of a doctor's bills. */
    @Query("SELECT b.visitDate, b.status, COUNT(b), SUM(b.discount) FROM Bill b WHERE b.doctorId = :doctorId "
            + "AND b.visitDate BETWEEN :from AND :to GROUP BY b.visitDate, b.status")
    List<Object[]> countAndDiscountPerDayAndStatus(@Param("doctorId") Long doctorId, @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    /** Rows of [visit date, status, sum of the lines] of the same bills. */
    @Query("SELECT b.visitDate, b.status, SUM(i.amount) FROM Bill b JOIN b.items i WHERE b.doctorId = :doctorId "
            + "AND b.visitDate BETWEEN :from AND :to GROUP BY b.visitDate, b.status")
    List<Object[]> linesPerDayAndStatus(@Param("doctorId") Long doctorId, @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    // ------------------------------------------------------------------ the daily cash summary (CashSummaryService)

    /** Bills paid or refunded from {@code from} (included) to {@code to} (not included), with their lines. */
    @Query("SELECT DISTINCT b FROM Bill b LEFT JOIN FETCH b.items WHERE (b.paidAt >= :from AND b.paidAt < :to) "
            + "OR (b.refundedAt >= :from AND b.refundedAt < :to)")
    List<Bill> findPaidOrRefundedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // A day's visits whose bills are still unpaid, with their lines
    @EntityGraph(attributePaths = "items")
    List<Bill> findByVisitDateAndStatus(LocalDate visitDate, BillStatus status);
}
