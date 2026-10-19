package com.itmonteur.hospitalerp.billing.web;

import com.itmonteur.hospitalerp.billing.internal.CashCloseDTO;
import com.itmonteur.hospitalerp.billing.internal.CashCloseService;
import com.itmonteur.hospitalerp.billing.internal.CashRangeDTO;
import com.itmonteur.hospitalerp.billing.internal.CashSummaryDTO;
import com.itmonteur.hospitalerp.billing.internal.CashSummaryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The daily cash summary (docs/DAILY_CASH_SUMMARY_PLAN.md). Today's for the desk under /api/receptionist (staff's
 * rules: receptionists and admins), and closing the day there; any day, or up to 31 days, under /api/admin
 * (administration's rules: admins).
 */
@RestController
public class CashSummaryController {

    public record CloseRequest(BigDecimal counted, String note) {
    }

    private final CashSummaryService cashSummaryService;
    private final CashCloseService cashCloseService;

    public CashSummaryController(CashSummaryService cashSummaryService, CashCloseService cashCloseService) {
        this.cashSummaryService = cashSummaryService;
        this.cashCloseService = cashCloseService;
    }

    @GetMapping("/api/receptionist/cash-summary")
    public ResponseEntity<CashSummaryDTO> today() {
        return ResponseEntity.ok(cashSummaryService.today());
    }

    // The cash counted now, and a note when it isn't what was expected
    @PostMapping("/api/receptionist/cash-closes")
    public ResponseEntity<CashCloseDTO> close(@RequestBody CloseRequest request) {
        return ResponseEntity.ok(cashCloseService.close(request.counted(), request.note()));
    }

    @GetMapping("/api/admin/cash-summary")
    public ResponseEntity<CashSummaryDTO> day(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(cashSummaryService.day(date));
    }

    @GetMapping("/api/admin/cash-summary/range")
    public ResponseEntity<CashRangeDTO> range(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(cashSummaryService.range(from, to));
    }
}
