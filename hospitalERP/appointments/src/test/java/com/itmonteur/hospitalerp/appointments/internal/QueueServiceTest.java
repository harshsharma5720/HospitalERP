package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentDTO;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.scheduling.Slot;
import com.itmonteur.hospitalerp.scheduling.SlotService;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.DoctorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Today's queue with a clock that doesn't move (docs/TODAYS_QUEUE_PLAN.md): two calls within one clock tick must still
 * have an order, or the screen can't tell which token is being served.
 */
class QueueServiceTest {

    // 12 Oct 2026, 10:00:00.1234567 in India - and it stays there
    private static final Clock FROZEN = Clock.fixed(Instant.parse("2026-10-12T04:30:00.1234567Z"), ZoneId.of("Asia/Kolkata"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    private QueueService queue;
    private Appointment first;
    private Appointment second;

    private static Appointment checkedIn(long id, Doctor doctor, int hour, int token) {
        Appointment appointment = new Appointment();
        appointment.setAppointmentID(id);
        appointment.setDoctor(doctor);
        appointment.setDate(TODAY);
        appointment.setSlot(new Slot(TODAY, LocalTime.of(hour, 0), LocalTime.of(hour, 10), doctor, null));
        appointment.setCheckedInAt(LocalDateTime.of(TODAY, LocalTime.of(9, 0)));
        appointment.setTokenNumber(token);
        return appointment;
    }

    @BeforeEach
    void setUp() {
        Doctor doctor = new Doctor();
        doctor.setId(9L);
        doctor.setName("Anita Rao");
        first = checkedIn(1, doctor, 10, 3);
        second = checkedIn(2, doctor, 11, 5);
        AppointmentRepository repository = mock(AppointmentRepository.class);
        when(repository.findByDoctor_IdAndDate(9L, TODAY)).thenReturn(List.of(first, second));
        when(repository.findCheckedInOn(TODAY)).thenReturn(List.of(first, second));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserService currentUser = mock(CurrentUserService.class);
        when(currentUser.hasRole(Role.DOCTOR)).thenReturn(true);
        when(currentUser.getCurrentUserId()).thenReturn(50L);
        DoctorService doctors = mock(DoctorService.class);
        when(doctors.findDoctorEntityByUserId(50L)).thenReturn(Optional.of(doctor));
        queue = new QueueService(repository, mock(SlotService.class), currentUser, doctors,
                mock(ApplicationEventPublisher.class), FROZEN);
    }

    @Test
    void twoCallsInOneClockTickStillHaveAnOrder() {
        AppointmentDTO calledFirst = queue.callNext();
        AppointmentDTO calledSecond = queue.callNext();

        assertThat(calledFirst.getTokenNumber()).isEqualTo(3);
        assertThat(calledSecond.getTokenNumber()).isEqualTo(5);
        assertThat(calledFirst.getCalledAt()).isEqualTo(LocalDateTime.of(TODAY, LocalTime.of(10, 0, 0, 123_456_000)));
        assertThat(calledSecond.getCalledAt()).isAfter(calledFirst.getCalledAt());
        assertThat(queue.screen().doctors().get(0).nowServing()).as("the last one called in").isEqualTo(5);
    }
}
