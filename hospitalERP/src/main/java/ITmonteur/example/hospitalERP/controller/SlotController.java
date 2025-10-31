package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.entities.Slot;
import ITmonteur.example.hospitalERP.entities.Shift;
import ITmonteur.example.hospitalERP.services.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private static final Logger logger = LoggerFactory.getLogger(SlotController.class);

    @Autowired
    private SlotService slotService;

    /**
     * Generate 10-minute slots for a specific doctor, date, and shift.
     * Example: POST /api/slots/generate/1?date=2025-11-01&shift=MORNING
     */
    @PostMapping("/generate/{doctorId}")
    public List<Slot> generateSlots(@PathVariable Long doctorId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam Shift shift) {
        logger.info("Request received to generate slots for doctorId: {}, date: {}, shift: {}", doctorId, date, shift);
        List<Slot> generatedSlots = this.slotService.generateSlots(doctorId, date, shift);
        logger.info("Successfully generated {} slots for doctorId: {}, shift: {}", generatedSlots.size(), doctorId, shift);
        return generatedSlots;
    }

    /**
     * Fetch all available slots for a given doctor, date, and shift.
     * Example: GET /api/slots/available/1?date=2025-11-01&shift=MORNING
     */
    @GetMapping("/available/{doctorId}")
    public List<Slot> getAvailableSlots(@PathVariable Long doctorId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                        @RequestParam Shift shift) {
        logger.info("Fetching available slots for doctorId: {}, date: {}, shift: {}", doctorId, date, shift);
        List<Slot> availableSlots = this.slotService.getAvailableSlots(doctorId, date, shift);
        logger.info("Found {} available slots for doctorId: {}, date: {}, shift: {}", availableSlots.size(), doctorId, date, shift);
        return availableSlots;
    }
}
