package com.itmonteur.hospitalerp.administration.internal;

import com.itmonteur.hospitalerp.appointments.AppointmentStatistics;
import com.itmonteur.hospitalerp.appointments.DailyAppointmentCounts;
import com.itmonteur.hospitalerp.appointments.DoctorAppointmentCount;
import com.itmonteur.hospitalerp.appointments.SpecializationCount;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.UserService;
import com.itmonteur.hospitalerp.notifications.NotificationOutbox;
import com.itmonteur.hospitalerp.staff.LeaveRequestService;
import com.itmonteur.hospitalerp.staff.Specialist;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** How the dashboard combines the modules' figures (docs/ADMIN_DASHBOARD_PLAN.md, step B.3). */
class DashboardServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);

    private final AppointmentStatistics statistics = mock(AppointmentStatistics.class);
    private final UserService userService = mock(UserService.class);
    private final LeaveRequestService leaveRequestService = mock(LeaveRequestService.class);
    private final NotificationOutbox notificationOutbox = mock(NotificationOutbox.class);
    private final DashboardService service = new DashboardService(statistics, userService, leaveRequestService, notificationOutbox,
            Clock.fixed(TODAY.atTime(15, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));

    /** Seven days ending today, zeros except the given ones. */
    private static List<DailyAppointmentCounts> week(DailyAppointmentCounts... days) {
        List<DailyAppointmentCounts> week = new ArrayList<>();
        for (LocalDate day = TODAY.minusDays(6); !day.isAfter(TODAY); day = day.plusDays(1)) {
            LocalDate date = day;
            week.add(List.of(days).stream().filter(d -> d.date().equals(date)).findFirst()
                    .orElse(new DailyAppointmentCounts(date, 0, 0, 0, 0)));
        }
        return week;
    }

    @Test
    void combinesTheFiguresOfTheWeek() {
        LocalDate twoDaysAgo = TODAY.minusDays(2);
        LocalDate yesterday = TODAY.minusDays(1);
        LocalDate from = TODAY.minusDays(6);
        when(statistics.perDay(from, TODAY)).thenReturn(week(
                new DailyAppointmentCounts(twoDaysAgo, 2, 1, 1, 0),  // completed, open, cancelled by patient, by doctor
                new DailyAppointmentCounts(yesterday, 1, 0, 0, 1),
                new DailyAppointmentCounts(TODAY, 1, 3, 1, 0)));
        when(userService.countNewAccountsPerDay(Role.PATIENT, from, TODAY)).thenReturn(Map.of(yesterday, 2L, TODAY, 1L));
        when(userService.countActiveAccountsPerRole()).thenReturn(
                Map.of(Role.PATIENT, 40L, Role.DOCTOR, 5L, Role.RECEPTIONIST, 2L, Role.ADMIN, 1L));
        when(userService.firstAccountCreationTime()).thenReturn(Optional.of(LocalDateTime.of(2026, 10, 6, 9, 0)));
        when(leaveRequestService.countDoctorsOnLeave(TODAY)).thenReturn(1L);
        List<SpecializationCount> specializations = List.of(new SpecializationCount(Specialist.CARDIOLOGY, 4));
        List<DoctorAppointmentCount> doctors = List.of(new DoctorAppointmentCount(7L, "Dr Rao", Specialist.CARDIOLOGY, 4, 3));
        when(statistics.busiestSpecializations(from, TODAY, 5)).thenReturn(specializations);
        when(statistics.busiestDoctors(from, TODAY, 5)).thenReturn(doctors);
        when(notificationOutbox.countUndelivered()).thenReturn(2L);

        DashboardDTO dashboard = service.dashboard(7);

        assertThat(dashboard.period()).isEqualTo(new DashboardDTO.Period(7, from, TODAY));
        assertThat(dashboard.trend()).hasSize(7);
        // Past days: open appointments were missed; today: they are upcoming
        assertThat(dashboard.trend().get(4)).isEqualTo(new DashboardDTO.Day(twoDaysAgo, 2, 0, 1, 1, 0));
        assertThat(dashboard.trend().get(5)).isEqualTo(new DashboardDTO.Day(yesterday, 1, 0, 0, 1, 2));
        assertThat(dashboard.trend().get(6)).isEqualTo(new DashboardDTO.Day(TODAY, 1, 3, 0, 1, 1));
        assertThat(dashboard.today()).isEqualTo(new DashboardDTO.Today(TODAY, 5, 1, 3, 1, 1, 40, 5, 2));
        // 11 appointments, 3 cancelled (2 by patients) = 27.3 %; past and not cancelled: 2+1 + 1 = 4, 1 missed = 25 %
        assertThat(dashboard.cancellations()).isEqualTo(new DashboardDTO.Cancellations(11, 3, 2, 1, 27.3, 1, 25.0));
        assertThat(dashboard.newPatients()).isEqualTo(new DashboardDTO.NewPatients(3, LocalDateTime.of(2026, 10, 6, 9, 0)));
        assertThat(dashboard.busiestSpecializations()).isSameAs(specializations);
        assertThat(dashboard.busiestDoctors()).isSameAs(doctors);
        assertThat(dashboard.undeliveredNotifications()).isEqualTo(2);
    }

    @Test
    void anEmptyPeriodHasNoRatesAndNoStartDate() {
        when(statistics.perDay(TODAY.minusDays(6), TODAY)).thenReturn(week());
        when(userService.countNewAccountsPerDay(any(), any(), any())).thenReturn(Map.of());
        when(userService.countActiveAccountsPerRole()).thenReturn(Map.of());
        when(userService.firstAccountCreationTime()).thenReturn(Optional.empty());

        DashboardDTO dashboard = service.dashboard(7);

        assertThat(dashboard.cancellations()).isEqualTo(new DashboardDTO.Cancellations(0, 0, 0, 0, 0.0, 0, 0.0));
        assertThat(dashboard.newPatients()).isEqualTo(new DashboardDTO.NewPatients(0, null));
        assertThat(dashboard.today()).isEqualTo(new DashboardDTO.Today(TODAY, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    @Test
    void onlySevenThirtyOrNinetyDays() {
        assertThatThrownBy(() -> service.dashboard(10)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.dashboard(0)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(statistics, userService, leaveRequestService, notificationOutbox);

        LocalDate from = TODAY.minusDays(89);
        List<DailyAppointmentCounts> quarter = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(TODAY); day = day.plusDays(1)) {
            quarter.add(new DailyAppointmentCounts(day, 0, 0, 0, 0));
        }
        when(statistics.perDay(from, TODAY)).thenReturn(quarter);
        when(userService.countActiveAccountsPerRole()).thenReturn(Map.of());
        when(userService.firstAccountCreationTime()).thenReturn(Optional.empty());

        assertThat(service.dashboard(90).period()).isEqualTo(new DashboardDTO.Period(90, from, TODAY));
    }

    @Test
    void percentagesHaveOneDecimal() {
        assertThat(DashboardService.percent(1, 3)).isEqualTo(33.3);
        assertThat(DashboardService.percent(2, 3)).isEqualTo(66.7);
        assertThat(DashboardService.percent(0, 0)).isEqualTo(0.0);
    }
}
