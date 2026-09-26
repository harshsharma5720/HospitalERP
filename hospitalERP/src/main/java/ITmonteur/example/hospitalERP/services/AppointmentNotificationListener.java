package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.events.AppointmentNotificationEvent;
import com.itmonteur.hospitalerp.notifications.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends appointment emails/SMS once the change is safely stored:
 * AFTER_COMMIT means nothing is sent if the transaction rolls back. (fallbackExecution: if an
 * event is ever published outside a transaction, it is still delivered.) NotificationService
 * itself is @Async, so this never slows down the request. Belongs to the appointments module.
 */
@Component
public class AppointmentNotificationListener {

    private final NotificationService notificationService;

    public AppointmentNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAppointmentNotification(AppointmentNotificationEvent event) {
        switch (event.kind()) {
            case BOOKED -> notificationService.appointmentBooked(event.info());
            case CANCELLED -> notificationService.appointmentCancelled(event.info());
            case CANCELLED_BY_DOCTOR_LEAVE -> notificationService.appointmentCancelledByDoctorLeave(event.info());
            case REMINDER -> notificationService.appointmentReminder(event.info());
        }
    }
}
