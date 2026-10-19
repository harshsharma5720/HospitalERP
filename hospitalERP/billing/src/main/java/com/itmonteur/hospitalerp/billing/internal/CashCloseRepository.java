package com.itmonteur.hospitalerp.billing.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CashCloseRepository extends JpaRepository<CashClose, Long> {

    // The last close: the next one covers the cash since it
    Optional<CashClose> findFirstByOrderByClosedAtDescIdDesc();

    // The closes made from one moment (included) to another (not included), oldest first
    List<CashClose> findByClosedAtGreaterThanEqualAndClosedAtLessThanOrderByClosedAtAscIdAsc(LocalDateTime from,
                                                                                               LocalDateTime to);
}
