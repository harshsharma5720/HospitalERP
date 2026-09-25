package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.events.DoctorScheduleChangedEvent;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * After a schedule change, removes the doctor's future slots that no appointment uses
 * (booked, cancelled or completed ones are kept), so they are regenerated from the new hours.
 * Only this module knows which slots bookings use, so it computes the list and asks the
 * scheduling module to delete the rest. Synchronous listener: runs in the schedule-update
 * transaction. Belongs to the appointments module.
 */
@Component
public class ScheduleChangeSlotCleaner {

    private final AppointmentRepository appointmentRepository;
    private final SlotService slotService;

    public ScheduleChangeSlotCleaner(AppointmentRepository appointmentRepository, SlotService slotService) {
        this.appointmentRepository = appointmentRepository;
        this.slotService = slotService;
    }

    @EventListener
    public void onScheduleChanged(DoctorScheduleChangedEvent event) {
        LocalDate today = LocalDate.now();
        List<Long> slotsInUse = appointmentRepository.findSlotIdsInUse(event.doctorId(), today);
        slotService.deleteUnusedSlots(event.doctorId(), today, slotsInUse);
    }
}
