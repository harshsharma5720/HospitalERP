package com.itmonteur.hospitalerp.billing.web;

import com.itmonteur.hospitalerp.billing.internal.OnlinePaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Razorpay's webhook (docs/ONLINE_PAYMENT_PLAN.md): set up in the Razorpay dashboard for {@code payment.captured},
 * with the secret in RAZORPAY_WEBHOOK_SECRET. Open to everyone (BillingSecurityRules); the signature decides.
 */
@RestController
public class RazorpayWebhookController {

    private final OnlinePaymentService onlinePaymentService;

    public RazorpayWebhookController(OnlinePaymentService onlinePaymentService) {
        this.onlinePaymentService = onlinePaymentService;
    }

    // The raw body: the signature is over the exact bytes Razorpay sent
    @PostMapping("/api/payments/razorpay/webhook")
    public ResponseEntity<Void> webhook(@RequestBody byte[] body,
                                        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        return ResponseEntity.status(switch (onlinePaymentService.webhook(body, signature)) {
            case ACCEPTED -> HttpStatus.OK;
            case REJECTED -> HttpStatus.BAD_REQUEST;
            case RETRY -> HttpStatus.SERVICE_UNAVAILABLE;
        }).build();
    }
}
