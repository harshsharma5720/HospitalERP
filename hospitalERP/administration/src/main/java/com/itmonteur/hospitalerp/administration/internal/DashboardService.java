package com.itmonteur.hospitalerp.administration.internal;

import com.itmonteur.hospitalerp.appointments.AppointmentStatistics;
import com.itmonteur.hospitalerp.appointments.DailyAppointmentCounts;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.UserService;
import com.itmonteur.hospitalerp.notifications.NotificationOutbox;
import com.itmonteur.hospitalerp.staff.LeaveRequestService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The admin dashboard (docs/ADMIN_DASHBOARD_PLAN.md): combines the figures that appointments, identity and staff
 * compute with their own count queries. Only 7, 30 or 90 days; the period ends today.
 */
@Service
public class DashboardService {

    static final Set<Integer> PERIODS = Set.of(7, 30, 90);
    static final int TOP = 5;

    private final AppointmentStatistics appointmentStatistics;
    private final UserService userService;
    private final LeaveRequestService leaveRequestService;
    private final NotificationOutbox notificationOutbox;
    private final Clock clock;

    public DashboardService(AppointmentStatistics appointmentStatistics, UserService userService,
                            LeaveRequestService leaveRequestService, NotificationOutbox notificationOutbox, Clock clock) {
        this.appointmentStatistics = appointmentStatistics;
        this.userService = userService;
        this.leaveRequestService = leaveRequestService;
        this.notificationOutbox = notificationOutbox;
        this.clock = clock;
    }

    public DashboardDTO dashboard(int days) {
        if (!PERIODS.contains(days)) {
            throw new BadRequestException("The period must be 7, 30 or 90 days");
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(days - 1L);
        List<DailyAppointmentCounts> perDay = appointmentStatistics.perDay(from, today);
        Map<LocalDate, Long> newPatientsPerDay = userService.countNewAccountsPerDay(Role.PATIENT, from, today);

        List<DashboardDTO.Day> trend = perDay.stream().map(day -> {
            boolean past = day.date().isBefore(today);
            return new DashboardDTO.Day(day.date(), day.completed(), past ? 0 : day.open(), past ? day.open() : 0,
                    day.cancelledByPatient() + day.cancelledByDoctor(), newPatientsPerDay.getOrDefault(day.date(), 0L));
        }).toList();

        return new DashboardDTO(new DashboardDTO.Period(days, from, today), today(today, perDay), trend,
                cancellations(perDay, trend), newPatients(trend),
                appointmentStatistics.busiestSpecializations(from, today, TOP),
                appointmentStatistics.busiestDoctors(from, today, TOP), notificationOutbox.countUndelivered());
    }

    private DashboardDTO.Today today(LocalDate today, List<DailyAppointmentCounts> perDay) {
        DailyAppointmentCounts counts = perDay.get(perDay.size() - 1); // the period ends today
        Map<Role, Long> active = userService.countActiveAccountsPerRole();
        return new DashboardDTO.Today(today, counts.total(), counts.completed(), counts.open(),
                counts.cancelledByPatient() + counts.cancelledByDoctor(), leaveRequestService.countDoctorsOnLeave(today),
                active.getOrDefault(Role.PATIENT, 0L), active.getOrDefault(Role.DOCTOR, 0L),
                active.getOrDefault(Role.RECEPTIONIST, 0L));
    }

    private static DashboardDTO.Cancellations cancellations(List<DailyAppointmentCounts> perDay,
                                                            List<DashboardDTO.Day> trend) {
        long appointments = perDay.stream().mapToLong(DailyAppointmentCounts::total).sum();
        long byPatient = perDay.stream().mapToLong(DailyAppointmentCounts::cancelledByPatient).sum();
        long byDoctor = perDay.stream().mapToLong(DailyAppointmentCounts::cancelledByDoctor).sum();
        List<DashboardDTO.Day> pastDays = trend.subList(0, trend.size() - 1); // the period ends today
        long missed = pastDays.stream().mapToLong(DashboardDTO.Day::missed).sum();
        long pastNotCancelled = pastDays.stream().mapToLong(day -> day.completed() + day.missed()).sum();
        return new DashboardDTO.Cancellations(appointments, byPatient + byDoctor, byPatient, byDoctor,
                percent(byPatient + byDoctor, appointments), missed, percent(missed, pastNotCancelled));
    }

    private DashboardDTO.NewPatients newPatients(List<DashboardDTO.Day> trend) {
        return new DashboardDTO.NewPatients(trend.stream().mapToLong(DashboardDTO.Day::newPatients).sum(),
                userService.firstAccountCreationTime().orElse(null));
    }

    /** part ÷ whole as a percentage with one decimal; 0 when whole is 0. */
    static double percent(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 1000.0 / whole) / 10.0;
    }
}
