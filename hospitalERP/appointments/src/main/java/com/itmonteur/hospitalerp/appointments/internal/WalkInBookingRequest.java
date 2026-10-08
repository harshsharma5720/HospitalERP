package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.patients.WalkInPatient;

/**
 * Front desk: book a slot for an existing patient ({@code patientId}) or for a new walk-in patient
 * ({@code newPatient}) - exactly one of the two. {@code age} is needed when no date of birth is known;
 * {@code message} is an optional note for the doctor.
 */
public record WalkInBookingRequest(Long patientId, WalkInPatient newPatient, Long slotId, Integer age, String message) {
}
