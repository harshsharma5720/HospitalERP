package com.itmonteur.hospitalerp.notifications;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Appointment emails and SMS to patient and doctor (docs/RELIABLE_NOTIFICATIONS_PLAN.md). Each message is queued
 * in the {@link NotificationOutbox} within the caller's transaction - so it exists exactly when the booking or
 * cancellation was committed - and sent right after the commit; a failed send is retried until the appointment.
 * A message without an address or phone number is left out. Methods take plain values, not entities.
 */
@Service
@Transactional
public class NotificationService {

    private static final Pattern TIME = Pattern.compile("(\\d{1,2}:\\d{2})");

    private final NotificationOutbox outbox;

    public NotificationService(NotificationOutbox outbox) {
        this.outbox = outbox;
    }

    /** {@code date} as yyyy-MM-dd; {@code time} as "09:00 - 09:10" (the start counts). */
    public record AppointmentInfo(String patientName, String patientEmail, String patientPhone,
                                  String doctorName, String doctorEmail, String doctorPhone,
                                  String date, String time) {}

    public void appointmentBooked(AppointmentInfo info) {
        LocalDateTime notAfter = appointmentStart(info);
        email(info.patientEmail(), AppointmentMessages.bookingEmailToPatient(info), "booking email to patient", notAfter);
        outbox.queueSms(info.patientPhone(), AppointmentMessages.bookingSmsToPatient(info), "booking SMS to patient", notAfter);
        email(info.doctorEmail(), AppointmentMessages.bookingEmailToDoctor(info), "booking email to doctor", notAfter);
        outbox.queueSms(info.doctorPhone(), AppointmentMessages.bookingSmsToDoctor(info), "booking SMS to doctor", notAfter);
    }

    public void appointmentCancelled(AppointmentInfo info) {
        LocalDateTime notAfter = appointmentStart(info);
        email(info.patientEmail(), AppointmentMessages.cancellationEmailToPatient(info), "cancellation email to patient", notAfter);
        outbox.queueSms(info.patientPhone(), AppointmentMessages.cancellationSmsToPatient(info), "cancellation SMS to patient", notAfter);
        outbox.queueSms(info.doctorPhone(), AppointmentMessages.cancellationSmsToDoctor(info), "cancellation SMS to doctor", notAfter);
    }

    public void appointmentCancelledByDoctorLeave(AppointmentInfo info) {
        LocalDateTime notAfter = appointmentStart(info);
        outbox.queueSms(info.patientPhone(), AppointmentMessages.leaveCancellationSmsToPatient(info), "leave cancellation SMS to patient", notAfter);
        email(info.patientEmail(), AppointmentMessages.leaveCancellationEmailToPatient(info), "leave cancellation email to patient", notAfter);
    }

    public void appointmentReminder(AppointmentInfo info) {
        LocalDateTime notAfter = appointmentStart(info);
        email(info.patientEmail(), AppointmentMessages.reminderEmailToPatient(info), "reminder email to patient", notAfter);
        outbox.queueSms(info.patientPhone(), AppointmentMessages.reminderSmsToPatient(info), "reminder SMS to patient", notAfter);
    }

    private void email(String to, AppointmentMessages.Email email, String description, LocalDateTime notAfter) {
        outbox.queueEmail(to, email.subject(), email.html(), description, notAfter);
    }

    /**
     * When the appointment starts - retries stop there. Without a time, the end of that day; without a readable
     * date, null (the outbox's 24-hour limit applies).
     */
    static LocalDateTime appointmentStart(AppointmentInfo info) {
        LocalDate date;
        try {
            date = LocalDate.parse(String.valueOf(info.date()).trim());
        } catch (DateTimeParseException e) {
            return null;
        }
        Matcher time = TIME.matcher(info.time() == null ? "" : info.time());
        if (time.find()) {
            try {
                return date.atTime(LocalTime.parse(time.group(1).length() == 4 ? "0" + time.group(1) : time.group(1)));
            } catch (DateTimeParseException ignored) {
                // fall through to the end of the day
            }
        }
        return date.plusDays(1).atStartOfDay();
    }
}
