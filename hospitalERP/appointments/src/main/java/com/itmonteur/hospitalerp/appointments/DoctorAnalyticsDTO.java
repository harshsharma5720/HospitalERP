package com.itmonteur.hospitalerp.appointments;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * One doctor's visits in a period (docs/DOCTOR_ANALYTICS_PLAN.md), by visit date, with the admin dashboard's rules:
 * an open visit is upcoming from today on and missed before; cancellation rate = cancelled / all visits, no-show rate
 * = missed / past visits that weren't cancelled (percent, one decimal).
 */
public record DoctorAnalyticsDTO(Period period, DoctorInfo doctor, Visits visits, List<Day> perDay,
                                 List<HourCount> byHour, List<WeekdayCount> byWeekday, Waiting waiting) {

    public record Period(int days, LocalDate from, LocalDate to) {
    }

    public record DoctorInfo(long id, String name, String specialist) {
    }

    public record Visits(long total, long completed, long missed, long upcoming, long cancelled,
                         double cancellationRate, double noShowRate) {
    }

    public record Day(LocalDate date, long completed, long missed, long upcoming, long cancelled) {
    }

    /** Visits (not cancelled) whose slot starts in that hour; every hour from the first to the last one with visits. */
    public record HourCount(int hour, long visits) {
    }

    /** Visits (not cancelled) on that weekday; all seven, Monday first. */
    public record WeekdayCount(DayOfWeek day, long visits) {
    }

    /**
     * From check-in to being called in, for the visits with both times (recorded since today's queue, 2026-10-12);
     * averages to one decimal, null when nothing was measured. {@code perDay}: every day of the period.
     */
    public record Waiting(long measured, Double averageMinutes, Long longestMinutes, List<WaitingDay> perDay) {
    }

    public record WaitingDay(LocalDate date, long measured, Double averageMinutes) {
    }
}
