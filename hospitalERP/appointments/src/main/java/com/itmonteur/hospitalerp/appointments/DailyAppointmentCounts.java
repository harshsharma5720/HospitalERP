package com.itmonteur.hospitalerp.appointments;

import java.time.LocalDate;

/**
 * Appointments on one day, by outcome (docs/ADMIN_DASHBOARD_PLAN.md). {@code open} = scheduled or confirmed and
 * not completed: upcoming if the day is today or later, missed if it has passed.
 */
public record DailyAppointmentCounts(LocalDate date, long completed, long open, long cancelledByPatient,
                                     long cancelledByDoctor) {

    public long total() {
        return completed + open + cancelledByPatient + cancelledByDoctor;
    }
}
