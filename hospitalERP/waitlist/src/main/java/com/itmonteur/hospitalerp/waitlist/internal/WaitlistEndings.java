package com.itmonteur.hospitalerp.waitlist.internal;

import com.itmonteur.hospitalerp.appointments.AppointmentNotificationEvent;
import com.itmonteur.hospitalerp.appointments.AppointmentService;
import com.itmonteur.hospitalerp.identity.UserDeactivatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Waits that end because of something else (docs/WAITLIST_PLAN.md), in that change's transaction: a booking (or a
 * move) with the doctor that day, and a deactivated patient or doctor. A removed family member's waits go with them
 * (the database deletes them).
 */
@Component
public class WaitlistEndings {

    private final WaitlistService waitlistService;
    private final AppointmentService appointmentService;

    public WaitlistEndings(WaitlistService waitlistService, AppointmentService appointmentService) {
        this.waitlistService = waitlistService;
        this.appointmentService = appointmentService;
    }

    // An event without an appointment (who: null) has nothing to end
    @EventListener
    public void onBooking(AppointmentNotificationEvent event) {
        if (event.who() == null) {
            return;
        }
        if (event.kind() == AppointmentNotificationEvent.Kind.BOOKED || event.kind() == AppointmentNotificationEvent.Kind.MOVED) {
            appointmentService.findAppointmentEntity(event.who().appointmentId()).ifPresent(waitlistService::closeBooked);
        }
    }

    @EventListener
    public void onDeactivated(UserDeactivatedEvent event) {
        waitlistService.endForAccount(event.userId(), event.role());
    }
}
