package com.itmonteur.hospitalerp.billing.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentService;
import com.itmonteur.hospitalerp.billing.BillNotificationEvent;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ConflictException;
import com.itmonteur.hospitalerp.common.LikePattern;
import com.itmonteur.hospitalerp.common.PageResult;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The front desk's billing (docs/BILLING_PLAN.md): the bills list, one bill, extra lines and a discount while unpaid,
 * the payment, the refund of a cancelled visit, and the receipt. Who took or gave back the money is kept on the bill.
 */
@Service
public class BillDeskService {

    private static final Logger logger = LoggerFactory.getLogger(BillDeskService.class);
    public static final int MAX_PAGE_SIZE = 100;
    private static final Pattern BILL_NUMBER = Pattern.compile("(?i)B-\\d{4}-\\d+");

    private final BillRepository billRepository;
    private final OnlinePaymentRepository onlinePayments;
    private final ReceiptPdfService receiptPdfService;
    private final CurrentUserService currentUserService;
    private final BillNotifications notifications;
    private final AppointmentService appointmentService;
    private final Clock clock;

    public BillDeskService(BillRepository billRepository, OnlinePaymentRepository onlinePayments,
                           ReceiptPdfService receiptPdfService, CurrentUserService currentUserService,
                           BillNotifications notifications, AppointmentService appointmentService, Clock clock) {
        this.billRepository = billRepository;
        this.onlinePayments = onlinePayments;
        this.receiptPdfService = receiptPdfService;
        this.currentUserService = currentUserService;
        this.notifications = notifications;
        this.appointmentService = appointmentService;
        this.clock = clock;
    }

    /**
     * Bills newest first. Filters, each optional: visit dates (both included), status (unpaid, paid, refund_due,
     * refunded, cancelled or all), and {@code patient}: part of the patient's name, or a bill number.
     */
    @Transactional(readOnly = true)
    public PageResult<BillSummaryDTO> list(LocalDate from, LocalDate to, String status, String patient, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Ask for page 0 or later, with 1 to " + MAX_PAGE_SIZE + " bills per page");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("The start date must not be after the end date");
        }
        BillStatus wanted = statusOf(status);
        String search = patient == null ? "" : patient.trim().replaceAll("\\s+", " ");
        Specification<Bill> matching = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (from != null) where.add(cb.greaterThanOrEqualTo(root.get("visitDate"), from));
            if (to != null) where.add(cb.lessThanOrEqualTo(root.get("visitDate"), to));
            if (wanted != null) where.add(cb.equal(root.get("status"), wanted));
            if (BILL_NUMBER.matcher(search).matches()) {
                where.add(cb.equal(root.get("billNumber"), search.toUpperCase(Locale.ROOT)));
            } else if (!search.isEmpty()) {
                where.add(cb.like(cb.lower(root.get("patientName")), LikePattern.containsIgnoringCase(search),
                        LikePattern.ESCAPE));
            }
            return cb.and(where.toArray(new Predicate[0]));
        };
        Page<Bill> found = billRepository.findAll(matching,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "visitDate", "id")));
        return PageResult.of(found.getContent().stream().map(BillSummaryDTO::of).toList(), page, size,
                found.getTotalElements());
    }

    @Transactional(readOnly = true)
    public BillDTO get(long billId) {
        return dto(find(billId));
    }

    /** The bill of a visit (made at check-in); 404 when it has none yet. */
    @Transactional(readOnly = true)
    public BillDTO forAppointment(long appointmentId) {
        return billRepository.findByAppointmentId(appointmentId).map(this::dto)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", "appointmentId", appointmentId));
    }

    /** An extra line (e.g. a dressing) on an unpaid bill: 1-200 characters, more than 0 rupees. */
    @Transactional
    public BillDTO addItem(long billId, String description, BigDecimal amount) {
        Bill bill = unpaid(billId);
        String text = description == null ? "" : description.trim();
        if (text.isEmpty() || text.length() > 200) {
            throw new BadRequestException("Please describe the item (at most 200 characters)");
        }
        BigDecimal money = FeeService.checkedAmount(amount, "amount");
        if (money.signum() == 0) {
            throw new BadRequestException("The amount must be more than 0");
        }
        bill.addItem(text, money, false);
        return dto(billRepository.saveAndFlush(bill));
    }

    /** Removes an extra line from an unpaid bill; the consultation fee stays (give a discount instead). */
    @Transactional
    public BillDTO removeItem(long billId, long itemId) {
        Bill bill = unpaid(billId);
        BillItem item = bill.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Bill item", "id", itemId));
        if (item.isConsultation()) {
            throw new BadRequestException("The consultation fee can't be removed; give a discount instead");
        }
        bill.getItems().remove(item);
        if (bill.getDiscount().compareTo(bill.subtotal()) > 0) {
            bill.setDiscount(bill.subtotal()); // never more than the bill
        }
        return dto(billRepository.saveAndFlush(bill));
    }

    /** The discount on an unpaid bill: 0 up to the bill's subtotal. */
    @Transactional
    public BillDTO setDiscount(long billId, BigDecimal discount) {
        Bill bill = unpaid(billId);
        BigDecimal money = FeeService.checkedAmount(discount, "discount");
        if (money.compareTo(bill.subtotal()) > 0) {
            throw new BadRequestException("The discount can't be more than the bill (Rs. " + bill.subtotal() + ")");
        }
        bill.setDiscount(money);
        return dto(billRepository.saveAndFlush(bill));
    }

    /** The patient paid at the desk: cash, card or UPI, with an optional reference (e.g. the UPI transaction id). */
    @Transactional
    public BillDTO pay(long billId, String method, String reference) {
        Bill bill = locked(billId);
        if (bill.getStatus() != BillStatus.UNPAID) {
            throw new ConflictException("This bill is " + label(bill.getStatus()) + "; it can't be paid");
        }
        bill.setPaymentMethod(methodOf(method));
        bill.setPaymentReference(referenceOf(reference));
        bill.setPaidAt(LocalDateTime.now(clock));
        bill.setPaidBy(currentUserService.getCurrentUser().getUsername());
        bill.setStatus(BillStatus.PAID);
        logger.info("Bill {} paid: {} by {}", bill.getBillNumber(), bill.total(), bill.getPaymentMethod());
        notifications.publish(BillNotificationEvent.Kind.PAID, bill);
        return dto(billRepository.saveAndFlush(bill));
    }

    /** The money of a cancelled, paid visit was given back. */
    @Transactional
    public BillDTO refund(long billId, String method, String reference) {
        Bill bill = locked(billId);
        if (bill.getStatus() != BillStatus.REFUND_DUE) {
            throw new ConflictException("Only a bill due for refund can be refunded; this one is " + label(bill.getStatus()));
        }
        bill.setRefundMethod(methodOf(method));
        bill.setRefundReference(referenceOf(reference));
        bill.setRefundedAt(LocalDateTime.now(clock));
        bill.setRefundedBy(currentUserService.getCurrentUser().getUsername());
        bill.setStatus(BillStatus.REFUNDED);
        logger.info("Bill {} refunded: {} by {}", bill.getBillNumber(), bill.total(), bill.getRefundMethod());
        notifications.publish(BillNotificationEvent.Kind.REFUNDED, bill);
        return dto(billRepository.saveAndFlush(bill));
    }

    /** The receipt of a paid bill (also once due for refund, or refunded). */
    @Transactional(readOnly = true)
    public byte[] receipt(long billId) {
        return receiptOf(find(billId));
    }

    /** The same rule for the patient's own receipts (MyBillService). */
    byte[] receiptOf(Bill bill) {
        if (bill.getStatus() == BillStatus.UNPAID || bill.getStatus() == BillStatus.CANCELLED) {
            throw new ConflictException("This bill is " + label(bill.getStatus()) + ": there is no receipt");
        }
        // The ABHA number of the person the visit was for, as it is now (docs/ABHA_NUMBER_PLAN.md)
        String abha = bill.getAppointmentId() == null ? null : appointmentService.findAppointmentEntity(bill.getAppointmentId())
                .map(Appointment::abhaNumber).orElse(null);
        return receiptPdfService.render(bill, abha);
    }

    /** The receipt's number, for its file name. */
    @Transactional(readOnly = true)
    public String billNumber(long billId) {
        return find(billId).getBillNumber();
    }

    // The bill with the online payments that couldn't pay it (docs/ONLINE_PAYMENT_PLAN.md)
    private BillDTO dto(Bill bill) {
        return BillDTO.of(bill, onlinePayments.findByBillIdAndStatusInOrderById(bill.getId(), List.of(
                OnlinePaymentStatus.REFUNDING, OnlinePaymentStatus.REFUNDED, OnlinePaymentStatus.REFUND_FAILED)));
    }

    private Bill find(long billId) {
        return billRepository.findById(billId).orElseThrow(() -> new ResourceNotFoundException("Bill", "id", billId));
    }

    // For a change: an online payment may be applied to the same bill meanwhile (OnlinePaymentService)
    private Bill locked(long billId) {
        return billRepository.findByIdForUpdate(billId).orElseThrow(() -> new ResourceNotFoundException("Bill", "id", billId));
    }

    private Bill unpaid(long billId) {
        Bill bill = locked(billId);
        if (bill.getStatus() != BillStatus.UNPAID) {
            throw new ConflictException("This bill is " + label(bill.getStatus()) + "; it can't be changed");
        }
        return bill;
    }

    private static String label(BillStatus status) {
        return status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static BillStatus statusOf(String status) {
        if (status == null || status.isBlank() || status.trim().equalsIgnoreCase("all")) {
            return null;
        }
        try {
            return BillStatus.valueOf(status.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("The status must be unpaid, paid, refund_due, refunded, cancelled or all");
        }
    }

    // At the desk: cash, card or UPI; ONLINE is only for payments made in the app (OnlinePaymentService)
    private static PaymentMethod methodOf(String method) {
        try {
            PaymentMethod chosen = PaymentMethod.valueOf(method == null ? "" : method.trim().toUpperCase(Locale.ROOT));
            if (chosen != PaymentMethod.ONLINE) {
                return chosen;
            }
        } catch (IllegalArgumentException e) {
            // the message below
        }
        throw new BadRequestException("Please choose how the money was paid: CASH, CARD or UPI");
    }

    private static String referenceOf(String reference) {
        String text = reference == null ? "" : reference.trim();
        if (text.length() > 100) {
            throw new BadRequestException("The reference can have at most 100 characters");
        }
        return text.isEmpty() ? null : text;
    }
}
