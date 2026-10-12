package com.itmonteur.hospitalerp.appointments;

/**
 * Today's queue (docs/TODAYS_QUEUE_PLAN.md): the patient of this appointment has checked in. Published inside the
 * check-in's transaction, once per check-in (checking in twice publishes nothing more). Billing makes the visit's bill.
 */
public record AppointmentCheckedInEvent(long appointmentId) {
}
