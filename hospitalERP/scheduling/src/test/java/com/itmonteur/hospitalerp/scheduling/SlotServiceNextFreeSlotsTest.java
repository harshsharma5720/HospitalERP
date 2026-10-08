package com.itmonteur.hospitalerp.scheduling;

import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.scheduling.internal.DoctorScheduleRepository;
import com.itmonteur.hospitalerp.scheduling.internal.SlotRepository;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.DoctorService;
import com.itmonteur.hospitalerp.staff.LeaveRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** The front desk's "next free slots" (docs/WALK_IN_REGISTRATION_PLAN.md, step W.2). */
class SlotServiceNextFreeSlotsTest {

    private final LocalDate tomorrow = LocalDate.now().plusDays(1);
    private SlotRepository slotRepository;
    private SlotService service;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        slotRepository = mock(SlotRepository.class);
        DoctorService doctorService = mock(DoctorService.class);
        doctor = new Doctor();
        doctor.setId(7L);
        when(doctorService.findDoctorEntity(7L)).thenReturn(Optional.of(doctor));
        // Free slots only tomorrow, returned in no particular order
        when(slotRepository.findByDoctorAndDateAndShiftAndAvailableTrue(any(), any(), any())).thenAnswer(inv -> {
            if (!tomorrow.equals(inv.getArgument(1))) {
                return List.of();
            }
            return inv.getArgument(2) == Shift.MORNING
                    ? List.of(slot(Shift.MORNING, 11), slot(Shift.MORNING, 9), slot(Shift.MORNING, 10))
                    : List.of(slot(Shift.EVENING, 16), slot(Shift.EVENING, 15));
        });
        service = new SlotService(slotRepository, doctorService, mock(LeaveRequestService.class),
                mock(DoctorScheduleRepository.class));
    }

    private Slot slot(Shift shift, int hour) {
        return new Slot(tomorrow, LocalTime.of(hour, 0), LocalTime.of(hour, 30), doctor, shift);
    }

    @Test
    void inTimeOrderUpToTheLimit() {
        assertThat(service.nextFreeSlots(7L, 4)).extracting(Slot::getStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(15, 0));
        // Fewer than asked for: whatever the booking window has
        assertThat(service.nextFreeSlots(7L, 20)).hasSize(5);
    }

    @Test
    void aDeactivatedDoctorHasNone() {
        User account = new User();
        account.setActive(false);
        doctor.setUser(account);

        assertThat(service.nextFreeSlots(7L, 5)).isEmpty();
        verify(slotRepository, never()).saveAll(any());
    }
}
