package com.itmonteur.hospitalerp.inbox.internal;

import com.itmonteur.hospitalerp.billing.BillNotificationEvent;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.inbox.Inbox;
import com.itmonteur.hospitalerp.inbox.InboxKind;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bills and payments in the notification centre (docs/NOTIFICATION_CENTRE_PLAN.md): to the patient a bill made, paid,
 * due for refund and refunded; to every receptionist a refund to give. Written in the bill's transaction.
 */
@Component
public class BillMessages {

    static final String PATIENT_PAGE = "/edit-profile"; // My account, with the bills

    private final Inbox inbox;

    public BillMessages(Inbox inbox) {
        this.inbox = inbox;
    }

    @EventListener
    public void on(BillNotificationEvent event) {
        String amount = Texts.rupees(event.amount());
        String visit = (event.doctorName() == null ? "the visit" : "the visit with Dr. " + event.doctorName()) + " on "
                + Texts.day(String.valueOf(event.visitDate()));
        Long patient = event.patientUserId();
        switch (event.kind()) {
            case BILL_READY -> inbox.toUser(patient, InboxKind.BILL_READY, "Your bill is ready", event.billNumber() + " for "
                    + visit + ": " + amount + ". Pay at the front desk, or online in My account.", PATIENT_PAGE);
            case PAID -> inbox.toUser(patient, InboxKind.PAYMENT_RECEIVED, "Payment received", amount + " for "
                    + event.billNumber() + ", by " + Texts.method(event.method()) + ". The receipt is in My account.",
                    PATIENT_PAGE);
            case REFUND_DUE -> {
                inbox.toUser(patient, InboxKind.REFUND_DUE, "Refund due", visit.substring(0, 1).toUpperCase()
                        + visit.substring(1) + " was cancelled: " + amount + " for " + event.billNumber()
                        + " will be given back.", PATIENT_PAGE);
                inbox.toRole(Role.RECEPTIONIST, InboxKind.REFUND_DUE, "Refund due: " + event.billNumber(),
                        event.patientName() + ", " + amount + ", paid by " + Texts.method(event.method()) + ". "
                                + visit.substring(0, 1).toUpperCase() + visit.substring(1) + " was cancelled.",
                        "/bills/" + event.billId());
            }
            case REFUNDED -> inbox.toUser(patient, InboxKind.REFUND_GIVEN, "Refund given", amount + " for "
                    + event.billNumber() + ", by " + Texts.method(event.method()) + ".", PATIENT_PAGE);
        }
    }
}
