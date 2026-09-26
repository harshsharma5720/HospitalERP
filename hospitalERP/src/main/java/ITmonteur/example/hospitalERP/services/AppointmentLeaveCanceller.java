package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.entities.AppointmentStatus;
import ITmonteur.example.hospitalERP.events.DoctorLeaveApprovedEvent;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.events.AppointmentNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cancels a doctor's upcoming bookings during an approved leave; the patients are notified
 * after the approval commits.
 * Already cancelled or completed appointments are left alone. Synchronous listener: runs in
 * the approval transaction. Belongs to the appointments module.
 */
@Component
public class AppointmentLeaveCanceller {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentLeaveCanceller.class);

    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentLeaveCanceller(AppointmentRepository appointmentRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void onDoctorLeaveApproved(DoctorLeaveApprovedEvent event) {
        List<Appointment> appointments = appointmentRepository
                .findByDoctor_IdAndDateBetween(event.doctorId(), event.startDate(), event.endDate());
        int cancelled = 0;
        for (Appointment appointment : appointments) {
            if (!appointment.isActive()) {
                continue;
            }
            appointment.setStatus(AppointmentStatus.CANCELLED_BY_DOCTOR);
            appointmentRepository.save(appointment);
            eventPublisher.publishEvent(new AppointmentNotificationEvent(
                    AppointmentNotificationEvent.Kind.CANCELLED_BY_DOCTOR_LEAVE,
                    AppointmentService.notificationInfo(appointment)));
            cancelled++;
        }
        logger.info("Doctor {} leave {}..{}: cancelled {} appointments",
                event.doctorId(), event.startDate(), event.endDate(), cancelled);
    }
}
