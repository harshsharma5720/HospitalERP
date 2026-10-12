package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.AppointmentDTO;
import com.itmonteur.hospitalerp.appointments.AppointmentService;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import com.itmonteur.hospitalerp.scheduling.SlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Walk-ins booked for today are checked in straight away (docs/TODAYS_QUEUE_PLAN.md, step Q.1). */
class FrontDeskServiceTest {

    // 11 Oct 2026, 09:30 in India
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-11T04:00:00Z"), ZoneId.of("Asia/Kolkata"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

    private AppointmentService appointments;
    private QueueService queue;
    private FrontDeskService service;

    @BeforeEach
    void setUp() {
        PtInfoService patients = mock(PtInfoService.class);
        PtInfo patient = new PtInfo();
        patient.setPatientId(7L);
        patient.setDob(LocalDate.of(1990, 1, 15));
        when(patients.findPatientEntity(7L)).thenReturn(Optional.of(patient));
        appointments = mock(AppointmentService.class);
        queue = mock(QueueService.class);
        service = new FrontDeskService(patients, appointments, mock(SlotService.class), mock(AppointmentRepository.class),
                queue, CLOCK);
    }

    private static AppointmentDTO booked(LocalDate date, Integer token) {
        AppointmentDTO appointment = new AppointmentDTO();
        appointment.setAppointmentID(90);
        appointment.setDate(date);
        appointment.setTokenNumber(token);
        return appointment;
    }

    @Test
    void aWalkInBookedForTodayIsCheckedInStraightAway() {
        when(appointments.createAppointment(any())).thenReturn(booked(TODAY, null));
        when(queue.checkIn(90L)).thenReturn(booked(TODAY, 4));

        WalkInBookingDTO result = service.registerAndBook(new WalkInBookingRequest(7L, null, 101L, null, null));

        assertThat(result.appointment().getTokenNumber()).isEqualTo(4);
        verify(queue).checkIn(90L);
    }

    @Test
    void aWalkInBookedForAnotherDayIsNot() {
        when(appointments.createAppointment(any())).thenReturn(booked(TODAY.plusDays(1), null));

        WalkInBookingDTO result = service.registerAndBook(new WalkInBookingRequest(7L, null, 101L, null, null));

        assertThat(result.appointment().getTokenNumber()).isNull();
        verifyNoInteractions(queue);
    }
}
