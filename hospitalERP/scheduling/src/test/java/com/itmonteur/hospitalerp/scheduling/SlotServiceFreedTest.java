package com.itmonteur.hospitalerp.scheduling;

import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.scheduling.internal.DoctorScheduleRepository;
import com.itmonteur.hospitalerp.scheduling.internal.SlotRepository;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.DoctorService;
import com.itmonteur.hospitalerp.staff.LeaveRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A freed time is published, and whether it can be booked now (docs/WAITLIST_PLAN.md, WL.2). */
class SlotServiceFreedTest {

    private final LocalDate tomorrow = LocalDate.now().plusDays(1);
    private SlotRepository slotRepository;
    private LeaveRequestService leaveService;
    private ApplicationEventPublisher publisher;
    private SlotService service;
    private Doctor doctor;
    private User account;

    @BeforeEach
    void setUp() {
        slotRepository = mock(SlotRepository.class);
        leaveService = mock(LeaveRequestService.class);
        publisher = mock(ApplicationEventPublisher.class);
        account = new User();
        account.setId(70L);
        account.setActive(true);
        doctor = new Doctor();
        doctor.setId(7L);
        doctor.setUser(account);
        service = new SlotService(slotRepository, mock(DoctorService.class), leaveService, mock(DoctorScheduleRepository.class),
                publisher);
    }

    private Slot slot(long id, LocalDate date, boolean available) {
        Slot slot = new Slot(date, LocalTime.of(10, 0), LocalTime.of(10, 30), doctor, Shift.MORNING);
        ReflectionTestUtils.setField(slot, "id", id); // set by the database
        slot.setAvailable(available);
        when(slotRepository.findById(id)).thenReturn(Optional.of(slot));
        return slot;
    }

    @Test
    void aReleasedTimeIsPublishedUnlessTheDoctorIsAway() {
        service.releaseSlot(slot(1L, tomorrow, false));
        verify(publisher).publishEvent(new SlotFreedEvent(1L, 7L, tomorrow, Shift.MORNING, LocalTime.of(10, 0), LocalTime.of(10, 30)));

        when(leaveService.isOnApprovedLeave(70L, tomorrow.plusDays(1))).thenReturn(true);
        Slot away = slot(2L, tomorrow.plusDays(1), false);
        service.releaseSlot(away);
        assertThat(away.isAvailable()).isFalse();
        verify(publisher, never()).publishEvent(new SlotFreedEvent(2L, 7L, tomorrow.plusDays(1), Shift.MORNING,
                LocalTime.of(10, 0), LocalTime.of(10, 30)));
        service.releaseSlot(null);
        verify(publisher).publishEvent(any(SlotFreedEvent.class)); // only the first
    }

    @Test
    void openForBookingOnlyWhenFreeNotStartedAndTheDoctorIsThere() {
        assertThat(service.isOpenForBooking(slot(1L, tomorrow, true).getId())).isTrue();
        assertThat(service.isOpenForBooking(slot(2L, tomorrow, false).getId())).as("booked").isFalse();
        assertThat(service.isOpenForBooking(slot(3L, LocalDate.now().minusDays(1), true).getId())).as("yesterday").isFalse();
        assertThat(service.isOpenForBooking(99L)).as("unknown").isFalse();

        when(leaveService.isOnApprovedLeave(70L, tomorrow)).thenReturn(true);
        assertThat(service.isOpenForBooking(1L)).as("away").isFalse();
        when(leaveService.isOnApprovedLeave(70L, tomorrow)).thenReturn(false);
        account.setActive(false);
        assertThat(service.isOpenForBooking(1L)).as("deactivated").isFalse();
    }
}
