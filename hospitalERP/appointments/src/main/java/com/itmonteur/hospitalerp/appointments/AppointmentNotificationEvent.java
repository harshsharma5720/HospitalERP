package com.itmonteur.hospitalerp.appointments;

import com.itmonteur.hospitalerp.notifications.NotificationService;

/**
 * "Tell the patient/doctor about this appointment". Published inside the transaction that
 * changes the appointment; AppointmentNotificationListener sends the email/SMS only AFTER that
 * transaction commits, so a booking that ends up rolled back never produces a message. The inbox
 * module writes its in-app messages in the same transaction (docs/NOTIFICATION_CENTRE_PLAN.md).
 * Belongs to the appointments module.
 *
 * @param kind what happened
 * @param info plain values captured while the entities were loaded (safe to use after commit)
 * @param who  the appointment and the users to tell
 */
public record AppointmentNotificationEvent(Kind kind, NotificationService.AppointmentInfo info, Who who) {
    public enum Kind {
        BOOKED,
        MOVED, // rescheduled: in the app only for now (no email / SMS)
        CANCELLED,
        CANCELLED_BY_DOCTOR_LEAVE,
        REMINDER
    }

    /** The appointment, the patient's login (the account holder's, also for a family member; null for a walk-in
     * without one) and the doctor's. */
    public record Who(long appointmentId, Long patientUserId, Long doctorUserId) {
    }
}
