package com.itmonteur.hospitalerp;

import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.events.DoctorLeaveApprovedEvent;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.repositories.SlotRepository;
import ITmonteur.example.hospitalERP.events.AppointmentNotificationEvent;
import ITmonteur.example.hospitalERP.services.AppointmentLeaveCanceller;
import ITmonteur.example.hospitalERP.services.SlotLeaveBlocker;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** The two listeners that react to an approved doctor leave (step 1.5). */
class DoctorLeaveListenersTest {

    private final LocalDate start = LocalDate.now().plusDays(1);
    private final LocalDate end = LocalDate.now().plusDays(2);
    private final DoctorLeaveApprovedEvent event = new DoctorLeaveApprovedEvent(7L, start, end);

    @Test
    void appointmentsInTheLeaveAreCancelledOnlyIfStillActive() {
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        Appointment active = appointment(AppointmentStatus.SCHEDULED);
        Appointment alreadyCancelled = appointment(AppointmentStatus.CANCELLED_BY_PATIENT);
        Appointment completed = appointment(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findByDoctor_IdAndDateBetween(7L, start, end))
                .thenReturn(List.of(active, alreadyCancelled, completed));

        new AppointmentLeaveCanceller(appointmentRepository, publisher).onDoctorLeaveApproved(event);

        assertThat(active.getStatus()).isEqualTo(AppointmentStatus.CANCELLED_BY_DOCTOR);
        assertThat(alreadyCancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED_BY_PATIENT);
        assertThat(completed.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(appointmentRepository, times(1)).save(active);
        verify(publisher, times(1)).publishEvent(
                notification(AppointmentNotificationEvent.Kind.CANCELLED_BY_DOCTOR_LEAVE));
    }

    @Test
    void slotsInTheLeaveAreBlocked() {
        SlotRepository slotRepository = mock(SlotRepository.class);
        Slot free = new Slot(start, LocalTime.of(9, 0), LocalTime.of(9, 10), new Doctor(), Shift.MORNING);
        Slot alsoFree = new Slot(end, LocalTime.of(15, 0), LocalTime.of(15, 10), new Doctor(), Shift.EVENING);
        when(slotRepository.findByDoctor_IdAndDateBetween(7L, start, end)).thenReturn(List.of(free, alsoFree));

        new SlotLeaveBlocker(slotRepository).onDoctorLeaveApproved(event);

        assertThat(free.isAvailable()).isFalse();
        assertThat(alsoFree.isAvailable()).isFalse();
        verify(slotRepository).saveAll(List.of(free, alsoFree));
    }

    private Appointment appointment(AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setStatus(status);
        appointment.setPatientName("P");
        appointment.setDate(start);
        return appointment;
    }

    // Matches an AppointmentNotificationEvent of the given kind passed to publishEvent(Object)
    private static Object notification(AppointmentNotificationEvent.Kind kind) {
        return argThat((Object e) -> e instanceof AppointmentNotificationEvent n && n.kind() == kind);
    }
}
