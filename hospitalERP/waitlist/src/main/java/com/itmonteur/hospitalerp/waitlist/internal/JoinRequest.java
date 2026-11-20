package com.itmonteur.hospitalerp.waitlist.internal;

import com.itmonteur.hospitalerp.scheduling.Shift;

import java.time.LocalDate;

/**
 * Joining a waitlist (docs/WAITLIST_PLAN.md): the doctor, the day, the shift (null: either) and who waits - the
 * patient ({@code patientId}: sent by the front desk; a patient's own account otherwise) or one of their family
 * members ({@code relativeId}).
 */
public record JoinRequest(Long patientId, Long relativeId, Long doctorId, LocalDate date, Shift shift) {
}
