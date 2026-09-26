package com.itmonteur.hospitalerp.appointments.internal;

import ITmonteur.example.hospitalERP.entities.*;
import com.itmonteur.hospitalerp.appointments.AppointmentNotificationEvent;
import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class AppointmentReminderServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-10T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void remindsTomorrowsAppointmentsAndMarksThemSent() {
        AppointmentRepository repo = mock(AppointmentRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        Appointment a = new Appointment();
        a.setPatientName("Asha");
        a.setDate(LocalDate.of(2026, 3, 11));
        a.setStatus(AppointmentStatus.SCHEDULED);
        when(repo.findDueForReminder(LocalDate.of(2026, 3, 11))).thenReturn(List.of(a));

        int sent = new AppointmentReminderService(repo, publisher, clock, true).sendDayBeforeReminders();

        assertThat(sent).isEqualTo(1);
        assertThat(a.isReminderSent()).isTrue();
        verify(publisher).publishEvent(notification(AppointmentNotificationEvent.Kind.REMINDER));
        verify(repo).saveAll(List.of(a));
    }

    @Test
    void disabledSchedulerDoesNothing() {
        AppointmentRepository repo = mock(AppointmentRepository.class);
        new AppointmentReminderService(repo, mock(ApplicationEventPublisher.class), clock, false).scheduledRun();
        verifyNoInteractions(repo);
    }

    // Matches an AppointmentNotificationEvent of the given kind passed to publishEvent(Object)
    private static Object notification(AppointmentNotificationEvent.Kind kind) {
        return argThat((Object e) -> e instanceof AppointmentNotificationEvent n && n.kind() == kind);
    }
}
