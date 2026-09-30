package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.patients.RelativeDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps appointment history when a relative is deleted: the appointments stay (with the
 * patient name they were booked under) and only the link to the relative is removed.
 * Synchronous listener: runs in the deleting transaction, before the relative row is removed.
 * Belongs to the appointments module.
 */
@Component
public class AppointmentRelativeUnlinker {

    private final AppointmentRepository appointmentRepository;

    public AppointmentRelativeUnlinker(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @EventListener
    public void onRelativeDeleted(RelativeDeletedEvent event) {
        appointmentRepository.clearRelative(event.relativeId());
    }
}
