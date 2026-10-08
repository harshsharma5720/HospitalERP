package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.AppointmentDTO;

/** The result of a front-desk booking: the patient (newly registered or not) and the appointment. */
public record WalkInBookingDTO(Long patientId, boolean newPatient, AppointmentDTO appointment) {
}
