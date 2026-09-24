package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Slot;
import ITmonteur.example.hospitalERP.entities.Shift;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ConflictException;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.LeaveRequestRepository;
import ITmonteur.example.hospitalERP.repositories.SlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlotService {

    private static final Logger logger = LoggerFactory.getLogger(SlotService.class);

    /** Patients can book this many days ahead (today included). */
    public static final int BOOKING_WINDOW_DAYS = 30;
    public static final int SLOT_MINUTES = 10;

    private final SlotRepository slotRepository;
    private final DoctorRepository doctorRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public SlotService(SlotRepository slotRepository, DoctorRepository doctorRepository,
                       LeaveRequestRepository leaveRequestRepository) {
        this.slotRepository = slotRepository;
        this.doctorRepository = doctorRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    /**
     * Creates 10-minute slots for a doctor/date/shift if they don't exist yet.
     * Synchronized (and not wrapped in an outer transaction) so two concurrent first
     * requests cannot generate the same slots twice on a single instance.
     */
    public synchronized List<Slot> generateSlots(Long doctorId, LocalDate date, Shift shift) {
        Doctor doctor = findDoctor(doctorId);
        List<Slot> existingSlots = slotRepository.findByDoctorAndDateAndShift(doctor, date, shift);
        if (!existingSlots.isEmpty()) {
            return existingSlots;
        }
        LocalTime start;
        LocalTime end;
        switch (shift) {
            case MORNING -> {
                start = LocalTime.of(9, 0);
                end = LocalTime.of(12, 0);
            }
            case EVENING -> {
                start = LocalTime.of(15, 0);
                end = LocalTime.of(19, 0);
            }
            default -> throw new BadRequestException("Invalid shift provided");
        }

        boolean onLeave = isDoctorOnLeave(doctor, date);
        List<Slot> slots = new ArrayList<>();
        while (start.isBefore(end)) {
            LocalTime slotEnd = start.plusMinutes(SLOT_MINUTES);
            Slot slot = new Slot(date, start, slotEnd, doctor, shift);
            slot.setAvailable(!onLeave);
            slots.add(slot);
            start = slotEnd;
        }
        logger.info("Generated {} slots for doctor {} on {} ({})", slots.size(), doctorId, date, shift);
        return slotRepository.saveAll(slots);
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
