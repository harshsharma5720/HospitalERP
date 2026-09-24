package ITmonteur.example.hospitalERP.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends appointment emails and SMS in the background.
 * Each channel is attempted independently and failures are only logged, so a slow or
 * failing mail server / Twilio account never breaks booking, cancelling or leave approval.
 * Methods take plain values (not entities) because they run outside the caller's transaction.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final EmailService emailService;
    private final SmsService smsService;

    public NotificationService(EmailService emailService, SmsService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public record AppointmentInfo(String patientName, String patientEmail, String patientPhone,
                                  String doctorName, String doctorEmail, String doctorPhone,
                                  String date, String time) {}

    @Async
    public void appointmentBooked(AppointmentInfo info) {
        attempt("booking email to patient", () -> emailService.sendBookingEmail(
                info.patientEmail(), info.patientName(), info.doctorName(), info.date(), info.time()));
        attempt("booking SMS to patient", () -> smsService.sendAppointmentSms(
                info.patientPhone(), info.patientName(), info.doctorName(), info.date(), info.time()));
        attempt("booking email to doctor", () -> emailService.sendDoctorNotificationEmail(
                info.doctorEmail(), info.patientName(), info.doctorName(), info.date(), info.time()));
        attempt("booking SMS to doctor", () -> smsService.sendDoctorSms(
                info.doctorPhone(), info.patientName(), info.doctorName(), info.date(), info.time()));
    }

    @Async
    public void appointmentCancelled(AppointmentInfo info) {
        attempt("cancellation email to patient", () -> emailService.sendDeleteEmail(
                info.patientEmail(), info.patientName(), info.doctorName(), info.date(), info.time()));
        attempt("cancellation SMS to patient", () -> smsService.sendAppointmentCancelSms(
                info.patientPhone(), info.patientName(), info.doctorName(), info.date(), info.time()));
        attempt("cancellation SMS to doctor", () -> smsService.sendDoctorCancelSms(
                info.doctorPhone(), info.patientName(), info.doctorName(), info.date(), info.time()));
    }

    @Async
    public void appointmentCancelledByDoctorLeave(AppointmentInfo info) {
        attempt("leave cancellation SMS", () -> smsService.sendDoctorLeaveCancelSms(
                info.patientPhone(), info.patientName(), info.doctorName(), info.date()));
        attempt("leave cancellation email", () -> emailService.sendDoctorLeaveCancelEmail(
                info.patientEmail(), info.patientName(), info.doctorName(), info.date()));
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private void attempt(String description, Action action) {
        try {
            action.run();
        } catch (Exception e) {
            logger.warn("Failed to send {}: {}", description, e.getMessage());
        }
    }
}
