package com.itmonteur.hospitalerp.scheduling;

import com.itmonteur.hospitalerp.scheduling.internal.DoctorScheduleRepository;
import com.itmonteur.hospitalerp.scheduling.internal.SlotRepository;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.DoctorService;
import com.itmonteur.hospitalerp.staff.LeaveRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Whether a shift has a time that can still free up, for the waitlist (docs/WAITLIST_PLAN.md). */
class SlotServiceTimesAheadTest {

    private final LocalDate today = LocalDate.now();
    private DoctorScheduleRepository scheduleRepository;
    private SlotService service;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(DoctorScheduleRepository.class);
        DoctorService doctorService = mock(DoctorService.class);
        Doctor doctor = new Doctor();
        doctor.setId(7L);
        when(doctorService.findDoctorEntity(7L)).thenReturn(Optional.of(doctor));
        service = new SlotService(mock(SlotRepository.class), doctorService, mock(LeaveRequestService.class), scheduleRepository,
                mock(ApplicationEventPublisher.class));
    }

    private void mornings(boolean working, LocalTime start, LocalTime end, int minutes) {
        DoctorSchedule s = new DoctorSchedule();
        s.setWorking(working);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setSlotMinutes(minutes);
        when(scheduleRepository.findByDoctor_IdAndDayOfWeekAndShift(eq(7L), any(), eq(Shift.MORNING))).thenReturn(Optional.of(s));
    }

    @Test
    void aWorkingShiftOnALaterDayHasTimesAhead() {
        mornings(true, LocalTime.of(0, 0), LocalTime.of(0, 10), 10); // whatever the time now
        assertThat(service.hasTimesAhead(7L, today.plusDays(1), Shift.MORNING)).isTrue();
    }

    @Test
    void noneOnADayOffInThePastOrWhenNoTimeFits() {
        mornings(false, LocalTime.of(10, 0), LocalTime.of(10, 30), 30);
        assertThat(service.hasTimesAhead(7L, today.plusDays(1), Shift.MORNING)).as("day off").isFalse();

        mornings(true, LocalTime.of(10, 0), LocalTime.of(10, 30), 30);
        assertThat(service.hasTimesAhead(7L, today.minusDays(1), Shift.MORNING)).as("yesterday").isFalse();

        mornings(true, LocalTime.of(10, 0), LocalTime.of(10, 20), 30);
        assertThat(service.hasTimesAhead(7L, today.plusDays(1), Shift.MORNING)).as("shorter than one time").isFalse();
    }

    @Test
    void todayOnlyWhileTheLastTimeHasNotStarted() {
        mornings(true, LocalTime.of(0, 0), LocalTime.of(0, 20), 10); // 00:00 and 00:10: both started
        assertThat(service.hasTimesAhead(7L, today, Shift.MORNING)).isFalse();

        assumeTrue(LocalTime.now().isBefore(LocalTime.of(23, 50)), "too close to midnight");
        mornings(true, LocalTime.of(0, 0), LocalTime.of(23, 59), 1); // the last one at 23:58
        assertThat(service.hasTimesAhead(7L, today, Shift.MORNING)).isTrue();
    }
}
