package com.itmonteur.hospitalerp.billing.internal;

import com.itmonteur.hospitalerp.billing.BillNotificationEvent;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes {@link BillNotificationEvent}s (docs/NOTIFICATION_CENTRE_PLAN.md), with the bill's patient account. */
@Component
public class BillNotifications {

    private final ApplicationEventPublisher events;
    private final PtInfoService ptInfoService;

    public BillNotifications(ApplicationEventPublisher events, PtInfoService ptInfoService) {
        this.events = events;
        this.ptInfoService = ptInfoService;
    }

    void publish(BillNotificationEvent.Kind kind, Bill bill) {
        Long patientUserId = bill.getPatientId() == null ? null : ptInfoService.findPatientEntity(bill.getPatientId())
                .map(patient -> patient.getUser() == null ? null : patient.getUser().getId()).orElse(null);
        PaymentMethod method = kind == BillNotificationEvent.Kind.REFUNDED ? bill.getRefundMethod()
                : kind == BillNotificationEvent.Kind.BILL_READY ? null : bill.getPaymentMethod();
        events.publishEvent(new BillNotificationEvent(kind, bill.getId(), bill.getBillNumber(), patientUserId,
                bill.getPatientName(), bill.getDoctorName(), bill.getVisitDate(), bill.total(),
                method == null ? null : method.name()));
    }
}
