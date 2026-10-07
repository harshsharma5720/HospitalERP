package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.AppointmentNotificationEvent;
import com.itmonteur.hospitalerp.notifications.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Queues the appointment emails/SMS in the notifications outbox, inside the transaction that books, cancels or
 * reminds (docs/RELIABLE_NOTIFICATIONS_PLAN.md): the messages are stored exactly when the change is committed -
 * a rolled-back change leaves none - and the outbox sends them right after the commit, retrying failures.
 * Belongs to the appointments module.
 */
@Component
public class AppointmentNotificationListener {

    private final NotificationService notificationService;

    public AppointmentNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void onAppointmentNotification(AppointmentNotificationEvent event) {
        switch (event.kind()) {
            case BOOKED -> notificationService.appointmentBooked(event.info());
            case CANCELLED -> notificationService.appointmentCancelled(event.info());
            case CANCELLED_BY_DOCTOR_LEAVE -> notificationService.appointmentCancelledByDoctorLeave(event.info());
            case REMINDER -> notificationService.appointmentReminder(event.info());
        }
    }
}
