package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Slot;
import ITmonteur.example.hospitalERP.entities.Shift;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ConflictException;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorScheduleRepository;
import ITmonteur.example.hospitalERP.repositories.LeaveRequestRepository;
import ITmonteur.example.hospitalERP.repositories.SlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SlotService {

    private static final Logger logger = LoggerFactory.getLogger(SlotService.class);

    /** Patients can book this many days ahead (today included). */
    public static final int BOOKING_WINDOW_DAYS = 30;

    private final SlotRepository slotRepository;
    private final DoctorRepository doctorRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    public SlotService(SlotRepository slotRepository, DoctorRepository doctorRepository,
                       LeaveRequestRepository leaveRequestRepository,
                       DoctorScheduleRepository doctorScheduleRepository) {
        this.slotRepository = slotRepository;
        this.doctorRepository = doctorRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
    }

    /**
     * Makes the stored slots for a doctor/date/shift match the doctor's schedule:
     * missing slots are created, and unbooked slots that are no longer part of the schedule
     * are blocked. Booked slots are never touched, so changing a schedule never drops bookings.
     * Synchronized (and not wrapped in an outer transaction) so two concurrent first requests
     * cannot create the same slots twice on a single instance.
     */
    public synchronized List<Slot> generateSlots(Long doctorId, LocalDate date, Shift shift) {
        Doctor doctor = findDoctor(doctorId);
        ScheduleDefaults.Hours hours = hoursFor(doctor, date, shift);
        boolean onLeave = isDoctorOnLeave(doctor, date);

        Map<String, Slot> existing = new HashMap<>();
        for (Slot slot : slotRepository.findByDoctorAndDateAndShift(doctor, date, shift)) {
            existing.put(key(slot.getStartTime(), slot.getEndTime()), slot);
        }

        Set<String> wanted = new HashSet<>();
        List<Slot> changed = new ArrayList<>();
        if (hours.working()) {
            int from = hours.start().toSecondOfDay() / 60;
            int to = hours.end().toSecondOfDay() / 60;
            for (int m = from; m + hours.slotMinutes() <= to; m += hours.slotMinutes()) {
                LocalTime slotStart = LocalTime.ofSecondOfDay(m * 60L);
                LocalTime slotEnd = LocalTime.ofSecondOfDay((m + hours.slotMinutes()) * 60L);
                String key = key(slotStart, slotEnd);
                wanted.add(key);
                if (!existing.containsKey(key)) {
                    Slot slot = new Slot(date, slotStart, slotEnd, doctor, shift);
                    slot.setAvailable(!onLeave);
                    changed.add(slot);
                }
            }
        }
        for (Map.Entry<String, Slot> entry : existing.entrySet()) {
            Slot slot = entry.getValue();
            if (!wanted.contains(entry.getKey()) && slot.isAvailable()) {
                slot.setAvailable(false); // outside the current schedule
                changed.add(slot);
            }
        }
        if (!changed.isEmpty()) {
            slotRepository.saveAll(changed);
            logger.debug("Reconciled {} slots for doctor {} on {} ({})", changed.size(), doctorId, date, shift);
        }
        return slotRepository.findByDoctorAndDateAndShift(doctor, date, shift);
    }

    /** The working hours that apply to a doctor on a given date and shift. */
    public ScheduleDefaults.Hours hoursFor(Doctor doctor, LocalDate date, Shift shift) {
        return doctorScheduleRepository.findByDoctor_IdAndDayOfWeekAndShift(doctor.getId(), date.getDayOfWeek(), shift)
                .map(s -> new ScheduleDefaults.Hours(s.isWorking(), s.getStartTime(), s.getEndTime(), s.getSlotMinutes()))
                .orElseGet(() -> ScheduleDefaults.forShift(shift));
    }

    /**
     * Removes today's and future slots that no appointment refers to, so they are regenerated
     * from the new schedule on the next request. Called after a schedule change.
     */
    @Transactional
    public void resetUnusedFutureSlots(Long doctorId) {
        slotRepository.deleteUnusedFromDate(doctorId, LocalDate.now());
    }

    private static String key(LocalTime start, LocalTime end) {
        return start + "-" + end;
    }

    /** Free slots for booking. Slots are generated on first request, so clients never need to create them. */
    public List<Slot> getAvailableSlots(Long doctorId, LocalDate date, Shift shift) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new BadRequestException("Cannot book appointments in the past");
        }
        if (date.isAfter(today.plusDays(BOOKING_WINDOW_DAYS - 1L))) {
            throw new BadRequestException("Appointments can be booked at most " + BOOKING_WINDOW_DAYS + " days ahead");
        }
        Doctor doctor = findDoctor(doctorId);
        if (isDoctorOnLeave(doctor, date)) {
            return List.of();
        }
        generateSlots(doctorId, date, shift);
        List<Slot> slots = new ArrayList<>(slotRepository.findByDoctorAndDateAndShiftAndAvailableTrue(doctor, date, shift));
        if (date.equals(today)) {
            LocalTime now = LocalTime.now();
            slots.removeIf(slot -> slot.getStartTime().isBefore(now));
        }
        return slots;
    }

    /** Locks the slot row and marks it booked. Must be called inside a transaction. */
    public Slot lockAndBook(Long slotId) {
        Slot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", "id", slotId));
        if (!slot.isAvailable()) {
            throw new ConflictException("This slot is no longer available. Please choose another one.");
        }
        LocalDate today = LocalDate.now();
        if (slot.getDate().isBefore(today)
                || (slot.getDate().equals(today) && slot.getStartTime().isBefore(LocalTime.now()))) {
            throw new BadRequestException("This slot is in the past. Please choose another one.");
        }
        slot.setAvailable(false);
        return slotRepository.save(slot);
    }

    /** Makes a slot bookable again, unless the doctor is on approved leave that day. */
    public void releaseSlot(Slot slot) {
        if (slot == null) {
            return;
        }
        slot.setAvailable(!isDoctorOnLeave(slot.getDoctor(), slot.getDate()));
        slotRepository.save(slot);
    }

    private boolean isDoctorOnLeave(Doctor doctor, LocalDate date) {
        return doctor != null && doctor.getUser() != null
                && leaveRequestRepository.isOnApprovedLeave(doctor.getUser().getId(), date);
    }

    private Doctor findDoctor(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
    }
}
