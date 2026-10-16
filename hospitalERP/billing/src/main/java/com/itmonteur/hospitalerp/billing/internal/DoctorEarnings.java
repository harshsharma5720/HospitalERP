package com.itmonteur.hospitalerp.billing.internal;

import com.itmonteur.hospitalerp.appointments.DoctorAnalytics;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.staff.DoctorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A doctor's earnings in a period (docs/DOCTOR_ANALYTICS_PLAN.md), next to the visit figures of the appointments
 * module's DoctorAnalytics: the logged-in doctor's, or any doctor's for an admin. The same periods (7, 30 or 90 days
 * ending today); two grouped queries.
 */
@Service
@Transactional(readOnly = true)
public class DoctorEarnings {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final BillRepository billRepository;
    private final DoctorService doctorService;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public DoctorEarnings(BillRepository billRepository, DoctorService doctorService,
                          CurrentUserService currentUserService, Clock clock) {
        this.billRepository = billRepository;
        this.doctorService = doctorService;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    /** The logged-in doctor's earnings; 404 for an account without a doctor profile. */
    public DoctorEarningsDTO mine(int days) {
        Long userId = currentUserService.getCurrentUserId();
        return of(doctorService.findDoctorEntityByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", userId)).getId(), days);
    }

    /** Any doctor's earnings (admins). */
    public DoctorEarningsDTO forDoctor(long doctorId, int days) {
        return of(doctorService.findDoctorEntity(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId)).getId(), days);
    }

    private record Key(LocalDate day, BillStatus status) {
    }

    private DoctorEarningsDTO of(long doctorId, int days) {
        if (!DoctorAnalytics.PERIODS.contains(days)) {
            throw new BadRequestException("The period must be 7, 30 or 90 days");
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(days - 1L);

        // Each (day, status): number of bills and total (the lines less the discounts)
        Map<Key, Long> counts = new HashMap<>();
        Map<Key, BigDecimal> totals = new HashMap<>();
        for (Object[] row : billRepository.countAndDiscountPerDayAndStatus(doctorId, from, today)) {
            Key key = new Key((LocalDate) row[0], (BillStatus) row[1]);
            counts.put(key, ((Number) row[2]).longValue());
            totals.merge(key, money(row[3]).negate(), BigDecimal::add);
        }
        for (Object[] row : billRepository.linesPerDayAndStatus(doctorId, from, today)) {
            totals.merge(new Key((LocalDate) row[0], (BillStatus) row[1]), money(row[2]), BigDecimal::add);
        }

        Map<LocalDate, BigDecimal> perDay = new TreeMap<>();
        for (LocalDate day = from; !day.isAfter(today); day = day.plusDays(1)) {
            perDay.put(day, ZERO);
        }
        BigDecimal earned = ZERO, refunds = ZERO, unpaid = ZERO;
        long paidVisits = 0, refundedVisits = 0, unpaidVisits = 0;
        for (Map.Entry<Key, BigDecimal> entry : totals.entrySet()) {
            Key key = entry.getKey();
            BigDecimal total = entry.getValue();
            long count = counts.getOrDefault(key, 0L);
            switch (key.status()) {
                case PAID -> {
                    earned = earned.add(total);
                    paidVisits += count;
                    perDay.merge(key.day(), total, BigDecimal::add);
                }
                case REFUND_DUE, REFUNDED -> {
                    refunds = refunds.add(total);
                    refundedVisits += count;
                }
                case UNPAID -> {
                    unpaid = unpaid.add(total);
                    unpaidVisits += count;
                }
                case CANCELLED -> {
                    // never paid
                }
            }
        }
        List<DoctorEarningsDTO.Day> daily = new ArrayList<>();
        perDay.forEach((day, amount) -> daily.add(new DoctorEarningsDTO.Day(day, amount)));
        BigDecimal average = paidVisits == 0 ? null
                : earned.divide(BigDecimal.valueOf(paidVisits), 2, RoundingMode.HALF_UP);
        return new DoctorEarningsDTO(new DoctorEarningsDTO.Period(days, from, today), earned, paidVisits, average,
                refunds, refundedVisits, unpaid, unpaidVisits, daily);
    }

    private static BigDecimal money(Object value) {
        return value == null ? ZERO : new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
