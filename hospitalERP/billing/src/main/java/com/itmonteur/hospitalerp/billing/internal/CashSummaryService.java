package com.itmonteur.hospitalerp.billing.internal;

import com.itmonteur.hospitalerp.common.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The daily cash summary (docs/DAILY_CASH_SUMMARY_PLAN.md), from the bills. A payment counts on the day it was made
 * ({@code paidAt}), whatever happened to the bill later; a refund on the day it was given ({@code refundedAt}), always
 * the whole bill. The amounts are the bills' totals, which can't change once paid.
 */
@Service
public class CashSummaryService {

    public static final int MAX_RANGE_DAYS = 31;
    static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    /** Money that moved: a payment or a refund of a bill. {@code by} is null for a payment made online by the patient. */
    record Move(LocalDateTime at, boolean payment, Bill bill, PaymentMethod method, String reference, String by,
                BigDecimal amount) {
    }

    private final BillRepository bills;
    private final CashCloseRepository closes;
    private final Clock clock;

    public CashSummaryService(BillRepository bills, CashCloseRepository closes, Clock clock) {
        this.bills = bills;
        this.closes = closes;
        this.clock = clock;
    }

    /** The desk's day: today. */
    @Transactional(readOnly = true)
    public CashSummaryDTO today() {
        return summaryOf(LocalDate.now(clock));
    }

    /** Any day up to today, for admins. */
    @Transactional(readOnly = true)
    public CashSummaryDTO day(LocalDate date) {
        requireNotAfterToday(date);
        return summaryOf(date);
    }

    /** From one day to another (both included, at most {@link #MAX_RANGE_DAYS}), for admins. */
    @Transactional(readOnly = true)
    public CashRangeDTO range(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("Choose the first and the last day");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("The first day must not be after the last day");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new BadRequestException("Choose at most " + MAX_RANGE_DAYS + " days");
        }
        requireNotAfterToday(to);
        List<Move> moves = moves(from, to);
        Map<LocalDate, List<Move>> perDay = new HashMap<>();
        moves.forEach(move -> perDay.computeIfAbsent(move.at().toLocalDate(), day -> new ArrayList<>()).add(move));
        List<CashCloseDTO> closed = closedOn(from, to);
        Map<LocalDate, List<CashCloseDTO>> closesPerDay = new HashMap<>();
        closed.forEach(close -> closesPerDay.computeIfAbsent(close.closedAt().toLocalDate(), day -> new ArrayList<>()).add(close));
        List<CashRangeDTO.Day> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<Move> ofDay = perDay.getOrDefault(date, List.of());
            CashSummaryDTO.Totals totals = totals(ofDay);
            List<Move> cash = ofDay.stream().filter(move -> move.method() == PaymentMethod.CASH).toList();
            List<CashCloseDTO> closesOfDay = closesPerDay.getOrDefault(date, List.of());
            BigDecimal difference = closesOfDay.isEmpty() ? null
                    : sum(closesOfDay.stream().map(CashCloseDTO::difference).toList());
            days.add(new CashRangeDTO.Day(date, totals.payments(), totals.taken(), totals.refunds(), totals.refunded(),
                    totals.net(), totals(cash).net(), closesOfDay.size(), difference));
        }
        return new CashRangeDTO(from, to, days, methods(moves), totals(moves), closed);
    }

    private CashSummaryDTO summaryOf(LocalDate date) {
        List<Move> moves = moves(date, date);
        List<Bill> unpaid = bills.findByVisitDateAndStatus(date, BillStatus.UNPAID);
        LocalDateTime now = LocalDateTime.now(clock);
        return new CashSummaryDTO(date, methods(moves), totals(moves), people(moves),
                moves.stream().map(CashSummaryService::line).toList(),
                new CashSummaryDTO.Unpaid(unpaid.size(), sum(unpaid.stream().map(Bill::total).toList())),
                closedOn(date, date), date.equals(now.toLocalDate()) ? cashSince(now) : null);
    }

    /** The cash since the last close up to {@code now}; with no close yet, since the start of {@code now}'s day. */
    CashSummaryDTO.CashSince cashSince(LocalDateTime now) {
        LocalDateTime from = closes.findFirstByOrderByClosedAtDescIdDesc().map(CashClose::getClosedAt)
                .orElse(now.toLocalDate().atStartOfDay());
        List<Move> cash = movesBetween(from, now).stream().filter(move -> move.method() == PaymentMethod.CASH).toList();
        CashSummaryDTO.Totals totals = totals(cash);
        return new CashSummaryDTO.CashSince(from, totals.payments(), totals.taken(), totals.refunds(), totals.refunded(),
                totals.net());
    }

    // The closes made on these days (both included), oldest first
    private List<CashCloseDTO> closedOn(LocalDate from, LocalDate to) {
        return closes.findByClosedAtGreaterThanEqualAndClosedAtLessThanOrderByClosedAtAscIdAsc(from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()).stream().map(CashCloseDTO::of).toList();
    }

    /** The payments and refunds of these days (both included), in the order they happened. */
    List<Move> moves(LocalDate from, LocalDate to) {
        return movesBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    /** The payments and refunds from {@code start} (included) to {@code end} (not included), in order. */
    List<Move> movesBetween(LocalDateTime start, LocalDateTime end) {
        Predicate<LocalDateTime> within = at -> at != null && !at.isBefore(start) && at.isBefore(end);
        List<Move> moves = new ArrayList<>();
        for (Bill bill : bills.findPaidOrRefundedBetween(start, end)) {
            if (within.test(bill.getPaidAt())) {
                moves.add(new Move(bill.getPaidAt(), true, bill, bill.getPaymentMethod(), bill.getPaymentReference(),
                        bill.getPaidBy(), bill.total()));
            }
            if (within.test(bill.getRefundedAt())) {
                moves.add(new Move(bill.getRefundedAt(), false, bill, bill.getRefundMethod(), bill.getRefundReference(),
                        bill.getRefundedBy(), bill.total()));
            }
        }
        moves.sort(Comparator.comparing(Move::at).thenComparing(move -> move.bill().getId())
                .thenComparing(move -> !move.payment()));
        return moves;
    }

    static List<CashSummaryDTO.MethodTotals> methods(List<Move> moves) {
        List<CashSummaryDTO.MethodTotals> methods = new ArrayList<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            CashSummaryDTO.Totals totals = totals(moves.stream().filter(move -> move.method() == method).toList());
            methods.add(new CashSummaryDTO.MethodTotals(method, totals.payments(), totals.taken(), totals.refunds(),
                    totals.refunded(), totals.net()));
        }
        return methods;
    }

    static CashSummaryDTO.Totals totals(List<Move> moves) {
        List<BigDecimal> taken = moves.stream().filter(Move::payment).map(Move::amount).toList();
        List<BigDecimal> refunded = moves.stream().filter(move -> !move.payment()).map(Move::amount).toList();
        return new CashSummaryDTO.Totals(taken.size(), sum(taken), refunded.size(), sum(refunded),
                sum(taken).subtract(sum(refunded)));
    }

    // Per person, by name; online payments (no one at the desk) last
    static List<CashSummaryDTO.PersonTotals> people(List<Move> moves) {
        Map<String, List<Move>> perPerson = new HashMap<>();
        moves.forEach(move -> perPerson.computeIfAbsent(move.by(), by -> new ArrayList<>()).add(move));
        return perPerson.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(entry -> {
                    CashSummaryDTO.Totals totals = totals(entry.getValue());
                    return new CashSummaryDTO.PersonTotals(entry.getKey(), totals.payments(), totals.taken(),
                            totals.refunds(), totals.refunded());
                })
                .toList();
    }

    private static CashSummaryDTO.Line line(Move move) {
        Bill bill = move.bill();
        return new CashSummaryDTO.Line(move.at(), move.payment() ? "PAYMENT" : "REFUND", bill.getId(),
                bill.getBillNumber(), bill.getPatientName(), bill.getDoctorName(), move.method(), move.reference(),
                move.by(), move.amount());
    }

    static BigDecimal sum(List<BigDecimal> amounts) {
        return amounts.stream().reduce(ZERO, BigDecimal::add);
    }

    private void requireNotAfterToday(LocalDate date) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Choose today or an earlier day");
        }
    }
}
