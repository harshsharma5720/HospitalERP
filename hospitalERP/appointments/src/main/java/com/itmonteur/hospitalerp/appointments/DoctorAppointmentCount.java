package com.itmonteur.hospitalerp.appointments;

import com.itmonteur.hospitalerp.staff.Specialist;

/** One doctor's appointments (not cancelled) in a period, and how many of them were completed. */
public record DoctorAppointmentCount(Long doctorId, String doctorName, Specialist specialist, long appointments,
                                     long completed) {
}
