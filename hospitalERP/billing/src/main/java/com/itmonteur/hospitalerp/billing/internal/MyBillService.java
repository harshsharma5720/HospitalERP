package com.itmonteur.hospitalerp.billing.internal;

import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * The logged-in patient's bills (docs/PATIENT_BILLS_PLAN.md): their own visits' and their family members' (a bill
 * keeps the account's patient id for both). Cancelled bills were never paid and are left out; another patient's
 * bill is "not found", so patients can't learn which bills exist.
 */
@Service
public class MyBillService {

    /** A receipt and its file name. */
    public record Receipt(String fileName, byte[] pdf) {
    }

    private final BillRepository billRepository;
    private final BillDeskService billDeskService;
    private final PtInfoService ptInfoService;
    private final CurrentUserService currentUserService;
    private final RazorpayClient razorpay;

    public MyBillService(BillRepository billRepository, BillDeskService billDeskService, PtInfoService ptInfoService,
                         CurrentUserService currentUserService, RazorpayClient razorpay) {
        this.billRepository = billRepository;
        this.billDeskService = billDeskService;
        this.ptInfoService = ptInfoService;
        this.currentUserService = currentUserService;
        this.razorpay = razorpay;
    }

    /** Newest visit first. */
    @Transactional(readOnly = true)
    public List<MyBillDTO> myBills() {
        return billRepository.findByPatientIdAndStatusNotOrderByVisitDateDescIdDesc(patientId(), BillStatus.CANCELLED)
                .stream().map(this::toDTO).toList();
    }

    /** One of the patient's bills, as they see it (after an online payment). */
    @Transactional(readOnly = true)
    public MyBillDTO myBill(long billId) {
        return toDTO(ownBill(billId));
    }

    /** A bill that can be paid online: unpaid, and at least Rs. 1 (Razorpay's smallest payment). */
    static boolean payable(Bill bill) {
        return bill.getStatus() == BillStatus.UNPAID && bill.total().compareTo(BigDecimal.ONE) >= 0;
    }

    /** The receipt of one of the patient's bills: paid, refund due or refunded, as at the desk. */
    @Transactional(readOnly = true)
    public Receipt receipt(long billId) {
        Bill bill = ownBill(billId);
        return new Receipt("receipt-" + bill.getBillNumber() + ".pdf", billDeskService.receiptOf(bill));
    }

    /** One of the logged-in patient's bills (not a cancelled one); another patient's is "not found". */
    Bill ownBill(long billId) {
        return billRepository.findByIdAndPatientIdAndStatusNot(billId, patientId(), BillStatus.CANCELLED)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", "id", billId));
    }

    PtInfo currentPatient() {
        Long userId = currentUserService.getCurrentUserId();
        return ptInfoService.findPatientEntityByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
    }

    Long patientId() {
        return currentPatient().getPatientId();
    }

    private MyBillDTO toDTO(Bill bill) {
        return MyBillDTO.of(bill, razorpay.enabled() && payable(bill));
    }
}
