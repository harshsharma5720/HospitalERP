package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.AppointmentDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * A doctor's queue today (docs/TODAYS_QUEUE_PLAN.md): called in and not finished yet (latest first), waiting
 * (checked in, by time), not arrived yet (by time) and done.
 */
public record DoctorQueueDTO(LocalDate date, List<AppointmentDTO> withDoctor, List<AppointmentDTO> waiting,
                             List<AppointmentDTO> notArrived, List<AppointmentDTO> done) {
}
