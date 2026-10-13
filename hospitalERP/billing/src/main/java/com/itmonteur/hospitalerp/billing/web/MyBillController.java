package com.itmonteur.hospitalerp.billing.web;

import com.itmonteur.hospitalerp.billing.internal.MyBillDTO;
import com.itmonteur.hospitalerp.billing.internal.MyBillService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The patient's own bills (docs/PATIENT_BILLS_PLAN.md). Patients only (BillingSecurityRules). */
@RestController
@RequestMapping("/api/bills/my")
public class MyBillController {

    private final MyBillService myBillService;

    public MyBillController(MyBillService myBillService) {
        this.myBillService = myBillService;
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
}
