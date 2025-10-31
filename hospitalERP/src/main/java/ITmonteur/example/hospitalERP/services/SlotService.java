package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Slot;
import ITmonteur.example.hospitalERP.entities.Shift;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlotService {

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    public List<Slot> generateSlots(Long doctorId, LocalDate date, Shift shift) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        List<Slot> existingSlots = slotRepository.findByDoctorAndDateAndShift(doctor, date, shift); // Check if slots already exist for this doctor, date, and shift
        if (!existingSlots.isEmpty()) {
            System.out.println("✅ Slots already exist for Doctor ID: " + doctorId +
                    " on " + date + " (" + shift + ")");
            return existingSlots; // Return existing slots instead of regenerating
        }
        List<Slot> slots = new ArrayList<>();// Generate new slots only if none exist
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
            default -> throw new IllegalArgumentException("Invalid shift provided");
        }

        while (start.isBefore(end)) {
            LocalTime slotEnd = start.plusMinutes(10);
            Slot slot = new Slot(date, start, slotEnd, doctor, shift);
            slots.add(slot);
            start = slotEnd;
        }

        System.out.println("🟢 Generated new slots for Doctor ID: " + doctorId +
                " on " + date + " (" + shift + ")");
        return slotRepository.saveAll(slots);
    }


//    public List<Slot> generateSlots(Long doctorId, LocalDate date, Shift shift) {
//        Doctor doctor = doctorRepository.findById(doctorId)
//                .orElseThrow(() -> new RuntimeException("Doctor not found"));
//
//        List<Slot> slots = new ArrayList<>();
//        LocalTime start = (shift == Shift.MORNING) ? LocalTime.of(9, 0) : LocalTime.of(15, 0);
//        LocalTime end = start.plusHours(3);
//
//        while (start.isBefore(end)) {
//            LocalTime slotEnd = start.plusMinutes(10);
//            Slot slot = new Slot(date, start, slotEnd, doctor, shift);
//            slots.add(slot);
//            start = slotEnd;
//        }
//        return slotRepository.saveAll(slots);
//    }

    public List<Slot> getAvailableSlots(Long doctorId, LocalDate date, Shift shift) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        return slotRepository.findByDoctorAndDateAndShiftAndAvailableTrue(doctor, date, shift);
    }

    public void bookSlot(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        slot.setAvailable(false);
        slotRepository.save(slot);
    }

    public void releaseSlot(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", "id", slotId));
        // Mark slot as available again
        slot.setAvailable(true);
        slotRepository.save(slot);

        System.out.println("Slot released successfully: " + slotId);
    }
}
