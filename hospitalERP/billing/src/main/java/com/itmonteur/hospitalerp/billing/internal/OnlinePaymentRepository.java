package com.itmonteur.hospitalerp.billing.internal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OnlinePaymentRepository extends JpaRepository<OnlinePayment, Long> {

    // Locked: the app's confirmation and Razorpay's webhook may apply the same payment at the same time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM OnlinePayment p WHERE p.orderId = :orderId")
    Optional<OnlinePayment> findByOrderIdForUpdate(@Param("orderId") String orderId);

    Optional<OnlinePayment> findByOrderId(String orderId);

    // An order still waiting for its payment, for the same amount (reused instead of making a new one)
    Optional<OnlinePayment> findFirstByBillIdAndStatusAndAmountOrderByIdDesc(Long billId, OnlinePaymentStatus status,
                                                                            BigDecimal amount);

    List<OnlinePayment> findByBillIdAndStatusInOrderById(Long billId, List<OnlinePaymentStatus> statuses);
}
