package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorScheduleRepository;
import ITmonteur.example.hospitalERP.repositories.LeaveRequestRepository;
import ITmonteur.example.hospitalERP.repositories.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SlotServiceScheduleTest {

    private DoctorScheduleRepository scheduleRepository;
    private SlotService service;
    private Doctor doctor;
    private final LocalDate monday = LocalDate.of(2026, 3, 9);
    // In-memory stand-in for the slots table
    private final List<Slot> stored = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        SlotRepository slotRepository = mock(SlotRepository.class);
        scheduleRepository = mock(DoctorScheduleRepository.class);
        DoctorRepository doctorRepository = mock(DoctorRepository.class);
        LeaveRequestRepository leaveRepository = mock(LeaveRequestRepository.class);
        doctor = new Doctor();
        doctor.setId(7L);
        when(doctorRepository.findById(7L)).thenReturn(Optional.of(doctor));
        when(slotRepository.findByDoctorAndDateAndShift(any(), any(), any())).thenAnswer(inv -> new ArrayList<>(stored));
        when(slotRepository.saveAll(any())).thenAnswer(inv -> {
            for (Slot s : (Iterable<Slot>) inv.getArgument(0)) {
                if (!stored.contains(s)) stored.add(s);
            }
            return inv.getArgument(0);
        });
        service = new SlotService(slotRepository, doctorRepository, leaveRepository, scheduleRepository);
    }

    private void schedule(boolean working, LocalTime start, LocalTime end, int minutes) {
        DoctorSchedule s = new DoctorSchedule();
        s.setWorking(working);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setSlotMinutes(minutes);
        when(scheduleRepository.findByDoctor_IdAndDayOfWeekAndShift(7L, monday.getDayOfWeek(), Shift.MORNING))
                .thenReturn(Optional.of(s));
    }

    @Test
    void withoutScheduleUsesHospitalDefaults() {
        List<Slot> slots = service.generateSlots(7L, monday, Shift.MORNING);
        assertThat(slots).hasSize(18); // 09:00-12:00 in 10-minute slots
        assertThat(slots.get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void customHoursAndSlotLengthAreUsed() {
        schedule(true, LocalTime.of(10, 0), LocalTime.of(12, 0), 30);
        List<Slot> slots = service.generateSlots(7L, monday, Shift.MORNING);
        assertThat(slots).extracting(Slot::getStartTime)
                .containsExactly(LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(11, 0), LocalTime.of(11, 30));
    }

    @Test
    void dayOffCreatesNoSlots() {
        schedule(false, null, null, 10);
        assertThat(service.generateSlots(7L, monday, Shift.MORNING)).isEmpty();
    }

    @Test
    void changedScheduleBlocksFreeSlotsButKeepsBookedOnes() {
        Slot free = new Slot(monday, LocalTime.of(9, 0), LocalTime.of(9, 10), doctor, Shift.MORNING);
        Slot booked = new Slot(monday, LocalTime.of(9, 10), LocalTime.of(9, 20), doctor, Shift.MORNING);
        booked.setAvailable(false);
        stored.add(free);
        stored.add(booked);
        schedule(true, LocalTime.of(10, 0), LocalTime.of(11, 0), 20);

        List<Slot> slots = service.generateSlots(7L, monday, Shift.MORNING);

        assertThat(free.isAvailable()).isFalse();   // outside the new hours
        assertThat(slots).contains(booked);         // booking untouched
        assertThat(slots).filteredOn(Slot::isAvailable).extracting(Slot::getStartTime)
                .containsExactly(LocalTime.of(10, 0), LocalTime.of(10, 20), LocalTime.of(10, 40));
    }
}
