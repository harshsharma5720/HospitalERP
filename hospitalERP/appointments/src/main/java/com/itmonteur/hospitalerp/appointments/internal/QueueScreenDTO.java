package com.itmonteur.hospitalerp.appointments.internal;

import java.time.LocalDate;
import java.util.List;

/**
 * The waiting-room screen (docs/TODAYS_QUEUE_PLAN.md): per doctor with a queue today, the token being served (the
 * last one called in) and the next waiting tokens. No patient data - it is shown without a login.
 */
public record QueueScreenDTO(LocalDate date, List<ScreenDoctor> doctors) {

    /** {@code specialist}: e.g. "CARDIOLOGY", or null when none is set; {@code nowServing}: null before anyone is called. */
    public record ScreenDoctor(String doctorName, String specialist, Integer nowServing, List<Integer> next) {
    }
}
