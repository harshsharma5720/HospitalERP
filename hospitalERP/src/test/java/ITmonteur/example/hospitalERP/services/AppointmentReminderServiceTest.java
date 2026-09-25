package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppointmentReminderServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-10T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void remindsTomorrowsAppointmentsAndMarksThemSent() {
        AppointmentRepository repo = mock(AppointmentRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        Appointment a = new Appointment();
        a.setPatientName("Asha");
        a.setDate(LocalDate.of(2026, 3, 11));
        a.setStatus(AppointmentStatus.SCHEDULED);
        when(repo.findDueForReminder(LocalDate.of(2026, 3, 11))).thenReturn(List.of(a));

        int sent = new AppointmentReminderService(repo, notifications, clock, true).sendDayBeforeReminders();

        assertThat(sent).isEqualTo(1);
        assertThat(a.isReminderSent()).isTrue();
        verify(notifications).appointmentReminder(any());
        verify(repo).saveAll(List.of(a));
    }

    @Test
    void disabledSchedulerDoesNothing() {
        AppointmentRepository repo = mock(AppointmentRepository.class);
        new AppointmentReminderService(repo, mock(NotificationService.class), clock, false).scheduledRun();
        verifyNoInteractions(repo);
    }
}
