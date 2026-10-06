package com.itmonteur.hospitalerp.appointments;

import com.itmonteur.hospitalerp.staff.Specialist;

/** Appointments (not cancelled) of one specialization in a period. */
public record SpecializationCount(Specialist specialist, long appointments) {
}
