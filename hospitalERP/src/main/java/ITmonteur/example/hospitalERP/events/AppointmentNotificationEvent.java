package ITmonteur.example.hospitalERP.events;

import ITmonteur.example.hospitalERP.services.NotificationService;

/**
 * "Tell the patient/doctor about this appointment". Published inside the transaction that
 * changes the appointment; AppointmentNotificationListener sends the email/SMS only AFTER that
 * transaction commits, so a booking that ends up rolled back never produces a message.
 * Belongs to the appointments module.
 *
 * @param kind what happened
 * @param info plain values captured while the entities were loaded (safe to use after commit)
 */
public record AppointmentNotificationEvent(Kind kind, NotificationService.AppointmentInfo info) {

    public enum Kind {
        BOOKED,
        CANCELLED,
        CANCELLED_BY_DOCTOR_LEAVE,
        REMINDER
    }
}
