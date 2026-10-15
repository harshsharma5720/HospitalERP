package com.itmonteur.hospitalerp.billing.web;

import com.itmonteur.hospitalerp.billing.internal.MyBillDTO;
import com.itmonteur.hospitalerp.billing.internal.MyBillService;
import com.itmonteur.hospitalerp.billing.internal.OnlinePaymentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The patient's own bills (docs/PATIENT_BILLS_PLAN.md), and paying them online (docs/ONLINE_PAYMENT_PLAN.md).
 * Patients only (BillingSecurityRules).
 */
@RestController
@RequestMapping("/api/bills/my")
public class MyBillController {

    /** Razorpay Checkout's answer: {@code razorpay_order_id}, {@code razorpay_payment_id}, {@code razorpay_signature}. */
    public record ConfirmRequest(String orderId, String paymentId, String signature) {
    }

    private final MyBillService myBillService;
    private final OnlinePaymentService onlinePaymentService;

    public MyBillController(MyBillService myBillService, OnlinePaymentService onlinePaymentService) {
        this.myBillService = myBillService;
        this.onlinePaymentService = onlinePaymentService;
    }

    // Their own and their family members' bills, newest first; cancelled ones left out
    @GetMapping
    public ResponseEntity<List<MyBillDTO>> list() {
        return ResponseEntity.ok(myBillService.myBills());
    }

    // 404 for someone else's bill, 409 before payment
    @GetMapping(value = "/{billId}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> receipt(@PathVariable long billId) {
        MyBillService.Receipt receipt = myBillService.receipt(billId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(receipt.fileName()).build().toString())
                .body(receipt.pdf());
    }

    // The Razorpay order for paying an unpaid bill (payOnline); 409 when it can't be paid online
    @PostMapping("/{billId}/online-payment")
    public ResponseEntity<OnlinePaymentService.CheckoutOrder> startOnlinePayment(@PathVariable long billId) {
        return ResponseEntity.ok(onlinePaymentService.start(billId));
    }

    // Checkout's answer: the paid bill; 400 for a wrong signature, 409 when the payment couldn't be applied (refunded)
    @PostMapping("/{billId}/online-payment/confirm")
    public ResponseEntity<MyBillDTO> confirmOnlinePayment(@PathVariable long billId, @RequestBody ConfirmRequest request) {
        return ResponseEntity.ok(onlinePaymentService.confirm(billId, request.orderId(), request.paymentId(),
                request.signature()));
    }
}
