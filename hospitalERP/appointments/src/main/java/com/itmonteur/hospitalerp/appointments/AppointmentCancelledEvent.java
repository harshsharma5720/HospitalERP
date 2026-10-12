package com.itmonteur.hospitalerp.appointments;

/**
 * An appointment was cancelled - by the patient or the front desk, by a doctor's approved leave, or because an
 * account was deactivated. Published inside that transaction. Billing cancels an unpaid bill and marks a paid one for
 * refund.
 */
public record AppointmentCancelledEvent(long appointmentId) {
}
