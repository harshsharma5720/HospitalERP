package com.itmonteur.hospitalerp.appointments;

/**
 * A checked-in patient left today's queue: the check-in was undone, or the appointment was rescheduled. Published
 * inside that transaction. Billing cancels the visit's bill if it is unpaid.
 */
public record AppointmentLeftQueueEvent(long appointmentId) {
}
