package com.itmonteur.hospitalerp.billing.internal;

import com.itmonteur.hospitalerp.audit.AuditAction;
import com.itmonteur.hospitalerp.audit.AuditLog;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ConflictException;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Closing the day at the front desk (docs/DAILY_CASH_SUMMARY_PLAN.md): the desk counts the cash, and the close keeps
 * it next to what the bills say the drawer should hold since the previous close. A difference needs a note. Every
 * close goes into the audit log.
 */
@Service
public class CashCloseService {

    private static final Logger logger = LoggerFactory.getLogger(CashCloseService.class);
    static final BigDecimal MAX_COUNTED = new BigDecimal("10000000.00"); // Rs. 1 crore
    static final int MAX_NOTE = 500;

    private final CashCloseRepository closes;
    private final CashSummaryService cashSummaryService;
    private final CurrentUserService currentUserService;
    private final AuditLog auditLog;
    private final Clock clock;

    public CashCloseService(CashCloseRepository closes, CashSummaryService cashSummaryService,
                            CurrentUserService currentUserService, AuditLog auditLog, Clock clock) {
        this.closes = closes;
        this.cashSummaryService = cashSummaryService;
        this.currentUserService = currentUserService;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    /** Closes now with the cash counted (Rs., at most 2 decimals) and a note, needed when it isn't what was expected. */
    @Transactional
    public CashCloseDTO close(BigDecimal counted, String note) {
        BigDecimal amount = countedOf(counted);
        String text = note == null ? "" : note.trim();
        if (text.length() > MAX_NOTE) {
            throw new BadRequestException("Please keep the note to " + MAX_NOTE + " characters");
        }
        // As the database keeps it (datetime(6)): the next close starts exactly here
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        CashSummaryDTO.CashSince since = cashSummaryService.cashSince(now);
        if (amount.compareTo(since.expected()) != 0 && text.isEmpty()) {
            throw new BadRequestException("The cash counted isn't what was expected (" + ReceiptPdfService.money(since.expected())
                    + "). Please say why in a note.");
        }
        CashClose close;
        try {
            close = closes.saveAndFlush(new CashClose(since, now, currentUserService.getCurrentUser().getUsername(),
                    amount, text.isEmpty() ? null : text));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Someone has just closed the day. Please look at the summary again.");
        }
        auditLog.record(AuditAction.CASH_DAY_CLOSED, null, close.getId(), "expected "
                + ReceiptPdfService.money(close.getCashExpected()) + ", counted " + ReceiptPdfService.money(amount)
                + ", difference " + ReceiptPdfService.money(close.getCashDifference()));
        logger.info("Day closed by {}: expected {}, counted {}", close.getClosedBy(), close.getCashExpected(), amount);
        return CashCloseDTO.of(close);
    }

    private static BigDecimal countedOf(BigDecimal counted) {
        if (counted == null) {
            throw new BadRequestException("Please enter the cash counted");
        }
        if (counted.signum() < 0) {
            throw new BadRequestException("The cash counted can't be below 0");
        }
        if (counted.stripTrailingZeros().scale() > 2) {
            throw new BadRequestException("Please enter the cash counted with at most 2 decimals");
        }
        if (counted.compareTo(MAX_COUNTED) > 0) {
            throw new BadRequestException("The cash counted can't be more than " + ReceiptPdfService.money(MAX_COUNTED));
        }
        return counted.setScale(2);
    }
}
