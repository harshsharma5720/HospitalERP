package com.itmonteur.hospitalerp.scheduling.internal;

import com.itmonteur.hospitalerp.scheduling.Slot;
import com.itmonteur.hospitalerp.staff.DoctorLeaveApprovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Blocks a doctor's existing slots during an approved leave. Slots generated later are
 * created blocked by SlotService itself. Synchronous listener: runs in the approval
 * transaction. Belongs to the scheduling module.
 */
@Component
public class SlotLeaveBlocker {

    private static final Logger logger = LoggerFactory.getLogger(SlotLeaveBlocker.class);

    private final SlotRepository slotRepository;

    public SlotLeaveBlocker(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @EventListener
    public void onDoctorLeaveApproved(DoctorLeaveApprovedEvent event) {
        List<Slot> slots = slotRepository
                .findByDoctor_IdAndDateBetween(event.doctorId(), event.startDate(), event.endDate());
        slots.forEach(slot -> slot.setAvailable(false));
        slotRepository.saveAll(slots);
        logger.info("Doctor {} leave {}..{}: blocked {} slots",
                event.doctorId(), event.startDate(), event.endDate(), slots.size());
    }
}
