package com.itmonteur.hospitalerp.administration.internal;

import com.itmonteur.hospitalerp.appointments.DoctorAppointmentCount;
import com.itmonteur.hospitalerp.appointments.SpecializationCount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * What GET /api/admin/dashboard returns (docs/ADMIN_DASHBOARD_PLAN.md). Appointments count on their appointment
 * date; rates are percentages with one decimal (0 when there is nothing to divide by).
 * {@code undeliveredNotifications}: appointment emails / SMS that failed for good and weren't resent
 * (docs/RELIABLE_NOTIFICATIONS_PLAN.md) - not limited to the period.
 */
public record DashboardDTO(Period period, Today today, List<Day> trend, Cancellations cancellations,
                           NewPatients newPatients, List<SpecializationCount> busiestSpecializations,
                           List<DoctorAppointmentCount> busiestDoctors, long undeliveredNotifications) {

    /** The last {@code days} days, ending today. */
    public record Period(int days, LocalDate from, LocalDate to) {
    }

    /** Today's appointments (all of them, also cancelled ones), doctors on leave, and active accounts. */
    public record Today(LocalDate date, long appointments, long completed, long upcoming, long cancelled,
                        long doctorsOnLeave, long activePatients, long activeDoctors, long activeReceptionists) {
    }

    /** One day of the period. Open appointments are upcoming from today on, missed before today. */
    public record Day(LocalDate date, long completed, long upcoming, long missed, long cancelled, long newPatients) {
    }

    /**
     * {@code cancellationRate} = cancelled ÷ all appointments of the period; {@code missedRate} = missed ÷ past
     * appointments that weren't cancelled.
     */
    public record Cancellations(long appointments, long cancelled, long cancelledByPatient, long cancelledByDoctor,
                                double cancellationRate, long missed, double missedRate) {
    }

    /** New patient accounts in the period; {@code countedSince} is null until the first account has a creation time. */
    public record NewPatients(long count, LocalDateTime countedSince) {
    }
}
