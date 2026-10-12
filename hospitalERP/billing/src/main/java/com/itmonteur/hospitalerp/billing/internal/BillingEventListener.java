package com.itmonteur.hospitalerp.billing.internal;

import com.itmonteur.hospitalerp.appointments.AppointmentCancelledEvent;
import com.itmonteur.hospitalerp.appointments.AppointmentCheckedInEvent;
import com.itmonteur.hospitalerp.appointments.AppointmentLeftQueueEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bills follow the visits (docs/BILLING_PLAN.md). Plain {@code @EventListener}s, so each bill change happens inside
 * the appointment's transaction: a check-in that fails leaves no bill, and a bill that can't be made fails the
 * check-in.
 */
@Component
public class BillingEventListener {

    private final BillService billService;

    public BillingEventListener(BillService billService) {
        this.billService = billService;
    }

    @EventListener
    public void onCheckedIn(AppointmentCheckedInEvent event) {
        billService.openBill(event.appointmentId());
    }

    @EventListener
    public void onLeftQueue(AppointmentLeftQueueEvent event) {
        billService.leftQueue(event.appointmentId());
    }

    @EventListener
    public void onCancelled(AppointmentCancelledEvent event) {
        billService.visitCancelled(event.appointmentId());
    }
}
