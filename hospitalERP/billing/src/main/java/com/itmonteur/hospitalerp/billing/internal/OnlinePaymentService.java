package com.itmonteur.hospitalerp.billing.internal;

import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ConflictException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.patients.PtInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Patients paying their bills online with Razorpay (docs/ONLINE_PAYMENT_PLAN.md). {@link #start} makes the Razorpay
 * order for an unpaid bill, Checkout takes the money, and {@link #confirm} checks Checkout's signature and applies the
 * payment. Applying is safe to repeat (the app and Razorpay's webhook may both do it) and locks the bill; a payment
 * that can't be applied any more (the bill was paid, changed or cancelled meanwhile) is refunded right away.
 * Razorpay is called outside the database transactions.
 */
@Service
public class OnlinePaymentService {

    private static final Logger logger = LoggerFactory.getLogger(OnlinePaymentService.class);

    /** What Razorpay Checkout needs; {@code amount} in paise. */
    public record CheckoutOrder(String keyId, String orderId, long amount, String currency, String name,
                                String description, Prefill prefill) {
    }

    /** The paying patient's details, filled in on Checkout. */
    public record Prefill(String name, String email, String contact) {
    }

    /** What became of a payment: it paid the bill, or it was (or is being) given back and why. */
    record Outcome(boolean paid, String message) {
    }

    private final RazorpayClient razorpay;
    private final OnlinePaymentRepository onlinePayments;
    private final BillRepository billRepository;
    private final MyBillService myBillService;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final String hospitalName;

    public OnlinePaymentService(RazorpayClient razorpay, OnlinePaymentRepository onlinePayments,
                                BillRepository billRepository, MyBillService myBillService,
                                TransactionTemplate transactions, Clock clock,
                                @Value("${app.hospital.name:Hospital ERP}") String hospitalName) {
        this.razorpay = razorpay;
        this.onlinePayments = onlinePayments;
        this.billRepository = billRepository;
        this.myBillService = myBillService;
        this.transactions = transactions;
        this.clock = clock;
        this.hospitalName = hospitalName;
    }

    private record Pending(long billId, String billNumber, BigDecimal amount, String orderId) {
    }

    /** The order for paying one of the patient's unpaid bills; an unused order for the same total is reused. */
    public CheckoutOrder start(long billId) {
        if (!razorpay.enabled()) {
            throw new ConflictException("Online payment isn't available. Please pay at the front desk.");
        }
        Pending pending = transactions.execute(status -> {
            Bill bill = myBillService.ownBill(billId);
            if (!MyBillService.payable(bill)) {
                throw new ConflictException(bill.getStatus() == BillStatus.UNPAID
                        ? "There is nothing to pay on this bill." : "This bill is already paid.");
            }
            String open = onlinePayments.findFirstByBillIdAndStatusAndAmountOrderByIdDesc(bill.getId(),
                    OnlinePaymentStatus.CREATED, bill.total()).map(OnlinePayment::getOrderId).orElse(null);
            return new Pending(bill.getId(), bill.getBillNumber(), bill.total(), open);
        });
        String orderId = pending.orderId();
        if (orderId == null) {
            RazorpayClient.Order order = atRazorpay(() -> razorpay.createOrder(paise(pending.amount()),
                    pending.billNumber(), Map.of("billNumber", pending.billNumber())));
            transactions.executeWithoutResult(status -> onlinePayments.save(new OnlinePayment(
                    billRepository.getReferenceById(pending.billId()), order.id(), pending.amount(), now())));
            orderId = order.id();
            logger.info("Bill {}: Razorpay order {} for {}", pending.billNumber(), orderId, pending.amount());
        }
        PtInfo patient = myBillService.currentPatient();
        return new CheckoutOrder(razorpay.keyId(), orderId, paise(pending.amount()), "INR", hospitalName,
                "Bill " + pending.billNumber(),
                new Prefill(patient.getPatientName(), patient.getEmail(), patient.getContactNo()));
    }

    /**
     * Checkout's answer for one of the patient's orders: the signature is checked, then the payment is applied.
     * Returns the paid bill; a payment that couldn't be applied → 409 saying it is refunded.
     */
    public MyBillDTO confirm(long billId, String orderId, String paymentId, String signature) {
        Long patientId = myBillService.patientId();
        OnlinePayment attempt = transactions.execute(status -> onlinePayments.findByOrderId(orderId == null ? "" : orderId)
                .filter(p -> p.getBill().getId() == billId && Objects.equals(p.getBill().getPatientId(), patientId))
                .orElseThrow(() -> new ResourceNotFoundException("Online payment", "orderId", orderId)));
        if (paymentId == null || paymentId.isBlank()
                || !razorpay.validPaymentSignature(attempt.getOrderId(), paymentId.trim(), signature)) {
            throw new BadRequestException("The payment couldn't be verified. If money was taken, please contact the front desk.");
        }
        Outcome outcome = apply(attempt.getOrderId(), paymentId.trim());
        if (!outcome.paid()) {
            throw new ConflictException(outcome.message());
        }
        return transactions.execute(status -> myBillService.myBill(billId));
    }

    /**
     * Applies a captured payment of one of our orders to its bill. Called by the app's confirmation and by Razorpay's
     * webhook, in either order and any number of times: a payment already handled gets the same outcome again.
     */
    Outcome apply(String orderId, String paymentId) {
        RazorpayClient.Payment payment = atRazorpay(() -> razorpay.fetchPayment(paymentId));
        if (!orderId.equals(payment.orderId())) {
            throw new BadRequestException("This payment belongs to another order");
        }
        if ("authorized".equals(payment.status())) {
            // Captured here, so the account's auto-capture setting doesn't matter
            payment = capture(payment);
        }
        boolean captured = "captured".equals(payment.status());
        RazorpayClient.Payment checked = payment;

        Decision decision = transactions.execute(status -> decide(orderId, checked, captured));
        if (decision.outcome() != null) {
            return decision.outcome();
        }
        // The payment can't be applied: give it back
        return refundUnapplied(decision.attemptId(), paymentId, payment.amount(), decision.reason());
    }

    /** Either the outcome, or a payment to refund (the attempt, and why). */
    private record Decision(Outcome outcome, Long attemptId, String reason) {
    }

    private Decision decide(String orderId, RazorpayClient.Payment payment, boolean captured) {
        OnlinePayment attempt = onlinePayments.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Online payment", "orderId", orderId));
        if (payment.id().equals(attempt.getPaymentId())) {
            return new Decision(recorded(attempt), null, null); // handled before
        }
        if (!captured) {
            throw new ConflictException("The payment didn't go through (" + payment.status()
                    + "). No money was taken; please try again.");
        }
        if (payment.amount() != paise(attempt.getAmount())) {
            throw new BadRequestException("The payment's amount doesn't match the order");
        }
        if (attempt.getStatus() != OnlinePaymentStatus.CREATED) {
            // Razorpay takes one payment per order; if a second one ever comes, it is given back unrecorded
            logger.error("Order {} was already handled; payment {} will be refunded", orderId, payment.id());
            return new Decision(null, null, "This bill was already paid");
        }
        Bill bill = billRepository.findByIdForUpdate(attempt.getBill().getId()).orElseThrow();
        attempt.setPaymentId(payment.id());
        String reason = unappliable(bill, attempt);
        if (reason != null) {
            attempt.setStatus(OnlinePaymentStatus.REFUNDING);
            attempt.setNote(reason);
            onlinePayments.saveAndFlush(attempt);
            return new Decision(null, attempt.getId(), reason);
        }
        LocalDateTime now = now();
        bill.setStatus(BillStatus.PAID);
        bill.setPaymentMethod(PaymentMethod.ONLINE);
        bill.setPaymentReference(payment.id());
        bill.setPaidAt(now);
        bill.setPaidBy(null); // paid by the patient through Razorpay, not at the desk
        attempt.setStatus(OnlinePaymentStatus.PAID);
        attempt.setPaidAt(now);
        billRepository.saveAndFlush(bill);
        onlinePayments.saveAndFlush(attempt);
        logger.info("Bill {} paid online: {} ({})", bill.getBillNumber(), bill.total(), payment.id());
        return new Decision(new Outcome(true, "Paid " + rupees(attempt.getAmount()) + "."), null, null);
    }

    // Why the payment can't pay the bill any more, or null when it can
    private static String unappliable(Bill bill, OnlinePayment attempt) {
        return switch (bill.getStatus()) {
            case UNPAID -> bill.total().compareTo(attempt.getAmount()) == 0 ? null
                    : "The bill changed after you started paying";
            case CANCELLED -> "This bill was cancelled";
            case PAID, REFUND_DUE, REFUNDED -> "This bill was already paid";
        };
    }

    private Outcome refundUnapplied(Long attemptId, String paymentId, long amount, String reason) {
        String money = rupees(BigDecimal.valueOf(amount, 2));
        try {
            RazorpayClient.Refund refund = atRazorpay(() -> razorpay.refund(paymentId, amount,
                    Map.of("reason", reason)));
            if (attemptId != null) {
                transactions.executeWithoutResult(status -> onlinePayments.findById(attemptId).ifPresent(attempt -> {
                    attempt.setStatus(OnlinePaymentStatus.REFUNDED);
                    attempt.setRefundId(refund.id());
                    attempt.setRefundedAt(now());
                }));
            }
            logger.warn("Online payment {} refunded ({}): {}", paymentId, refund.id(), reason);
            return new Outcome(false, reason + ", so your payment of " + money
                    + " is being refunded to you (refund " + refund.id() + ", 5-7 working days).");
        } catch (RuntimeException e) {
            if (attemptId != null) {
                transactions.executeWithoutResult(status -> onlinePayments.findById(attemptId)
                        .ifPresent(attempt -> attempt.setStatus(OnlinePaymentStatus.REFUND_FAILED)));
            }
            logger.error("Online payment {} could not be refunded ({}): {}", paymentId, reason, e.getMessage());
            return new Outcome(false, reason + ", and your payment of " + money + " couldn't be refunded "
                    + "automatically. Please show the payment id " + paymentId + " at the front desk.");
        }
    }

    // The outcome of a payment handled before
    private static Outcome recorded(OnlinePayment attempt) {
        String money = rupees(attempt.getAmount());
        return switch (attempt.getStatus()) {
            case PAID -> new Outcome(true, "Paid " + money + ".");
            case REFUNDING, CREATED -> new Outcome(false, attempt.getNote() + ", so your payment of " + money
                    + " is being refunded to you.");
            case REFUNDED -> new Outcome(false, attempt.getNote() + ", so your payment of " + money
                    + " is being refunded to you (refund " + attempt.getRefundId() + ", 5-7 working days).");
            case REFUND_FAILED -> new Outcome(false, attempt.getNote() + ", and your payment of " + money
                    + " couldn't be refunded automatically. Please show the payment id " + attempt.getPaymentId()
                    + " at the front desk.");
        };
    }

    // An authorized payment captured; if it was captured meanwhile (e.g. by the account's setting), it is fetched again
    private RazorpayClient.Payment capture(RazorpayClient.Payment payment) {
        try {
            return atRazorpay(() -> razorpay.capture(payment.id(), payment.amount()));
        } catch (ResponseStatusException e) {
            return atRazorpay(() -> razorpay.fetchPayment(payment.id()));
        }
    }

    // A Razorpay call; when Razorpay can't be reached or refuses, the caller gets 502 with a plain message
    private static <T> T atRazorpay(Supplier<T> call) {
        try {
            return call.get();
        } catch (RazorpayClient.RazorpayException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The payment service couldn't be reached. Please try again in a moment.");
        }
    }

    static long paise(BigDecimal rupees) {
        return rupees.movePointRight(2).longValueExact();
    }

    private static String rupees(BigDecimal amount) {
        return String.format(Locale.ENGLISH, "₹%,.2f", amount);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
