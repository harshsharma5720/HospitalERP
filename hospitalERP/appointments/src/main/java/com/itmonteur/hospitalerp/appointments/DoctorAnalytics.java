package com.itmonteur.hospitalerp.appointments;

import com.itmonteur.hospitalerp.appointments.internal.AppointmentRepository;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.DoctorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A doctor's visits in numbers (docs/DOCTOR_ANALYTICS_PLAN.md): per day, the no-show and cancellation rates, busy
 * hours and weekdays, and the wait from check-in to being called in. For the logged-in doctor, or any doctor for an
 * admin. Only 7, 30 or 90 days; the period ends today.
 */
@Service
@Transactional(readOnly = true)
public class DoctorAnalytics {

    public static final Set<Integer> PERIODS = Set.of(7, 30, 90);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatistics statistics;
    private final DoctorService doctorService;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public DoctorAnalytics(AppointmentRepository appointmentRepository, AppointmentStatistics statistics,
                           DoctorService doctorService, CurrentUserService currentUserService, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.statistics = statistics;
        this.doctorService = doctorService;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    /** The logged-in doctor's figures; 404 for an account without a doctor profile. */
    public DoctorAnalyticsDTO mine(int days) {
        Long userId = currentUserService.getCurrentUserId();
        return of(doctorService.findDoctorEntityByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", userId)), days);
    }

    /** Any doctor's figures (admins). */
    public DoctorAnalyticsDTO forDoctor(long doctorId, int days) {
        return of(doctorService.findDoctorEntity(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId)), days);
    }

    private DoctorAnalyticsDTO of(Doctor doctor, int days) {
        if (!PERIODS.contains(days)) {
            throw new BadRequestException("The period must be 7, 30 or 90 days");
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(days - 1L);
        long doctorId = doctor.getId();

        List<DoctorAnalyticsDTO.Day> perDay = new ArrayList<>();
        long completed = 0, missed = 0, upcoming = 0, cancelled = 0, pastCompleted = 0;
        for (DailyAppointmentCounts day : statistics.perDayForDoctor(doctorId, from, today)) {
            boolean past = day.date().isBefore(today);
            long dayCancelled = day.cancelledByPatient() + day.cancelledByDoctor();
            perDay.add(new DoctorAnalyticsDTO.Day(day.date(), day.completed(), past ? day.open() : 0,
                    past ? 0 : day.open(), dayCancelled));
            completed += day.completed();
            missed += past ? day.open() : 0;
            upcoming += past ? 0 : day.open();
            cancelled += dayCancelled;
            pastCompleted += past ? day.completed() : 0;
        }
        long total = completed + missed + upcoming + cancelled;
        DoctorAnalyticsDTO.Visits visits = new DoctorAnalyticsDTO.Visits(total, completed, missed, upcoming, cancelled,
                percent(cancelled, total), percent(missed, pastCompleted + missed));

        Busy busy = busy(doctorId, from, today);
        return new DoctorAnalyticsDTO(new DoctorAnalyticsDTO.Period(days, from, today), info(doctor), visits, perDay,
                busy.byHour(), busy.byWeekday(), waiting(doctorId, from, today));
    }

    private static DoctorAnalyticsDTO.DoctorInfo info(Doctor doctor) {
        String name = doctor.getName();
        if ((name == null || name.isBlank()) && doctor.getUser() != null) {
            name = doctor.getUser().getUsername(); // a profile not filled in yet
        }
        return new DoctorAnalyticsDTO.DoctorInfo(doctor.getId(), name,
                doctor.getSpecialist() == null ? null : doctor.getSpecialist().name());
    }

    private record Busy(List<DoctorAnalyticsDTO.HourCount> byHour, List<DoctorAnalyticsDTO.WeekdayCount> byWeekday) {
    }

    // Visits by the hour their slot starts and by weekday, from one query
    private Busy busy(long doctorId, LocalDate from, LocalDate to) {
        TreeMap<Integer, Long> hours = new TreeMap<>();
        long[] weekdays = new long[7];
        for (Object[] row : appointmentRepository.countPerDayAndStartTimeForDoctor(doctorId, from, to)) {
            long count = ((Number) row[2]).longValue();
            hours.merge(((LocalTime) row[1]).getHour(), count, Long::sum);
            weekdays[((LocalDate) row[0]).getDayOfWeek().ordinal()] += count;
        }
        List<DoctorAnalyticsDTO.HourCount> byHour = new ArrayList<>();
        if (!hours.isEmpty()) {
            for (int hour = hours.firstKey(); hour <= hours.lastKey(); hour++) {
                byHour.add(new DoctorAnalyticsDTO.HourCount(hour, hours.getOrDefault(hour, 0L)));
            }
        }
        List<DoctorAnalyticsDTO.WeekdayCount> byWeekday = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            byWeekday.add(new DoctorAnalyticsDTO.WeekdayCount(day, weekdays[day.ordinal()]));
        }
        return new Busy(byHour, byWeekday);
    }

    private DoctorAnalyticsDTO.Waiting waiting(long doctorId, LocalDate from, LocalDate to) {
        Map<LocalDate, List<Long>> perDay = new TreeMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            perDay.put(day, new ArrayList<>());
        }
        List<Long> all = new ArrayList<>();
        for (Object[] row : appointmentRepository.queueTimesForDoctor(doctorId, from, to)) {
            long minutes = Math.max(0, Duration.between((LocalDateTime) row[1], (LocalDateTime) row[2]).toMinutes());
            perDay.get((LocalDate) row[0]).add(minutes);
            all.add(minutes);
        }
        List<DoctorAnalyticsDTO.WaitingDay> days = new ArrayList<>();
        perDay.forEach((day, minutes) -> days.add(new DoctorAnalyticsDTO.WaitingDay(day, minutes.size(), average(minutes))));
        return new DoctorAnalyticsDTO.Waiting(all.size(), average(all),
                all.stream().max(Long::compare).orElse(null), days);
    }

    private static Double average(List<Long> minutes) {
        return minutes.isEmpty() ? null
                : Math.round(minutes.stream().mapToLong(Long::longValue).average().orElse(0) * 10) / 10.0;
    }

    // Percent with one decimal, like the admin dashboard
    static double percent(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 1000.0 / whole) / 10.0;
    }
}
