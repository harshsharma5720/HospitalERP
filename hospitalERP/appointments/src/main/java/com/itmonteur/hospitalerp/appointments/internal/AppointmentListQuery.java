package com.itmonteur.hospitalerp.appointments.internal;

import java.time.LocalDate;

/**
 * The front desk's appointment list filters, each optional (docs/PATIENT_SEARCH_PLAN.md): dates (both included),
 * doctor, status ("upcoming", "completed", "cancelled" or "all"), part of the patient's name or one patient, and the
 * order ("asc": oldest first, the default; "desc": newest first).
 */
public record AppointmentListQuery(LocalDate from, LocalDate to, Long doctorId, String status, String patient,
                                   Long patientId, String order) {
}
