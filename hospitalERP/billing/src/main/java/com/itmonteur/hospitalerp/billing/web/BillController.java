package com.itmonteur.hospitalerp.billing.web;

import com.itmonteur.hospitalerp.billing.internal.BillDTO;
import com.itmonteur.hospitalerp.billing.internal.BillDeskService;
import com.itmonteur.hospitalerp.billing.internal.OnlinePaymentService;
import com.itmonteur.hospitalerp.billing.internal.BillSummaryDTO;
import com.itmonteur.hospitalerp.common.PageResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The front desk's billing (docs/BILLING_PLAN.md). Under /api/receptionist, so RECEPTIONIST or ADMIN (staff's access
 * rules).
 */
@RestController
@RequestMapping("/api/receptionist/bills")
public class BillController {

    public record ItemRequest(String description, BigDecimal amount) {
    }

    public record DiscountRequest(BigDecimal discount) {
    }

    /** {@code method}: CASH, CARD or UPI; {@code reference}: optional, e.g. the UPI transaction id. */
    public record PaymentRequest(String method, String reference) {
    }

    private final BillDeskService billDeskService;
    private final OnlinePaymentService onlinePaymentService;

    public BillController(BillDeskService billDeskService, OnlinePaymentService onlinePaymentService) {
        this.billDeskService = billDeskService;
        this.onlinePaymentService = onlinePaymentService;
    }

    // Newest first; every filter optional. patient: part of the name, or a bill number
    @GetMapping
    public ResponseEntity<PageResult<BillSummaryDTO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String patient,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(billDeskService.list(from, to, status, patient, page, size));
    }

    @GetMapping("/{billId}")
    public ResponseEntity<BillDTO> get(@PathVariable long billId) {
        return ResponseEntity.ok(billDeskService.get(billId));
    }

    // The bill of a visit (from the appointment list); 404 before check-in
    @GetMapping("/by-appointment/{appointmentId}")
    public ResponseEntity<BillDTO> forAppointment(@PathVariable long appointmentId) {
        return ResponseEntity.ok(billDeskService.forAppointment(appointmentId));
    }

    @PostMapping("/{billId}/items")
    public ResponseEntity<BillDTO> addItem(@PathVariable long billId, @RequestBody ItemRequest request) {
        return ResponseEntity.ok(billDeskService.addItem(billId, request.description(), request.amount()));
    }

    @DeleteMapping("/{billId}/items/{itemId}")
    public ResponseEntity<BillDTO> removeItem(@PathVariable long billId, @PathVariable long itemId) {
        return ResponseEntity.ok(billDeskService.removeItem(billId, itemId));
    }

    @PutMapping("/{billId}/discount")
    public ResponseEntity<BillDTO> setDiscount(@PathVariable long billId, @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(billDeskService.setDiscount(billId, request.discount()));
    }

    @PostMapping("/{billId}/pay")
    public ResponseEntity<BillDTO> pay(@PathVariable long billId, @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(billDeskService.pay(billId, request.method(), request.reference()));
    }

    @PostMapping("/{billId}/refund")
    public ResponseEntity<BillDTO> refund(@PathVariable long billId, @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(billDeskService.refund(billId, request.method(), request.reference()));
    }

    // A cancelled visit paid online: refunded through Razorpay (docs/ONLINE_PAYMENT_PLAN.md)
    @PostMapping("/{billId}/refund-online")
    public ResponseEntity<BillDTO> refundOnline(@PathVariable long billId) {
        return ResponseEntity.ok(onlinePaymentService.refundOnline(billId));
    }

    @GetMapping(value = "/{billId}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> receipt(@PathVariable long billId) {
        byte[] pdf = billDeskService.receipt(billId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("receipt-" + billDeskService.billNumber(billId) + ".pdf").build().toString())
                .body(pdf);
    }
}
