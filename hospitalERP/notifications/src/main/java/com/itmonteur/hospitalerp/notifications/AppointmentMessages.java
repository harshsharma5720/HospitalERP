package com.itmonteur.hospitalerp.notifications;

import com.itmonteur.hospitalerp.notifications.NotificationService.AppointmentInfo;
import org.springframework.web.util.HtmlUtils;

/**
 * The texts of the appointment emails and SMS. They are rendered when a message is queued
 * (docs/RELIABLE_NOTIFICATIONS_PLAN.md), so a queued message never changes. Names and other user-supplied values
 * are HTML-escaped in emails.
 */
public final class AppointmentMessages {

    /** A rendered email: subject and the full HTML (layout included). */
    public record Email(String subject, String html) {
    }

    private AppointmentMessages() {
    }

    // ------------------------------------------------------------------ emails

    public static Email bookingEmailToPatient(AppointmentInfo info) {
        return email("Appointment Confirmation - Hospital ERP",
                "<h2 style='color:#2E86C1;'>Appointment Confirmed ✔</h2>"
                        + "<p>Dear <strong>" + esc(info.patientName()) + "</strong>,</p>"
                        + "<p>Your appointment has been successfully booked with the following details:</p>"
                        + details("Doctor", info.doctorName(), info)
                        + "<p style='margin-top:10px;'>Please reach the hospital <strong>10 minutes before</strong> the scheduled time.</p>");
    }

    public static Email bookingEmailToDoctor(AppointmentInfo info) {
        return email("New Appointment Scheduled - Hospital ERP",
                "<h2 style='color:#2E86C1;'>New Appointment Scheduled 📢</h2>"
                        + "<p>Dear <strong>Dr. " + esc(info.doctorName()) + "</strong>,</p>"
                        + "<p>A new appointment has been scheduled with the following patient:</p>"
                        + details("Patient Name", info.patientName(), info)
                        + "<p>Please review the appointment details and be prepared accordingly.</p>");
    }

    public static Email cancellationEmailToPatient(AppointmentInfo info) {
        return email("Appointment Cancelled - Hospital ERP",
                "<h2 style='color:#C0392B;'>Appointment Cancelled ❌</h2>"
                        + "<p>Dear <strong>" + esc(info.patientName()) + "</strong>,</p>"
                        + "<p>Your appointment has been cancelled.</p>"
                        + details("Doctor", info.doctorName(), info)
                        + "<p>If this was a mistake, please book again from the portal.</p>");
    }

    public static Email leaveCancellationEmailToPatient(AppointmentInfo info) {
        return email("Appointment Cancelled Due to Doctor Leave - Hospital ERP",
                "<h2 style='color:#C0392B;'>Appointment Cancelled ❌</h2>"
                        + "<p>Dear <strong>" + esc(info.patientName()) + "</strong>,</p>"
                        + "<p>Your appointment with <strong>Dr. " + esc(info.doctorName())
                        + "</strong> has been cancelled due to doctor's leave.</p>"
                        + "<table style='width:100%; font-size:14px;'>"
                        + "<tr><td><strong>Date:</strong></td><td>" + esc(info.date()) + "</td></tr>"
                        + "</table>"
                        + "<p>Please <strong>rebook</strong> your appointment from the portal.</p>");
    }

    public static Email reminderEmailToPatient(AppointmentInfo info) {
        return email("Appointment Reminder - Hospital ERP",
                "<h2 style='color:#2E86C1;'>Appointment Reminder ⏰</h2>"
                        + "<p>Dear <strong>" + esc(info.patientName()) + "</strong>,</p>"
                        + "<p>This is a reminder of your appointment <strong>tomorrow</strong>:</p>"
                        + details("Doctor", "Dr. " + info.doctorName(), info)
                        + "<p style='margin-top:10px;'>Please reach the hospital <strong>10 minutes before</strong> the scheduled time. "
                        + "If you can't make it, please cancel or reschedule from the portal.</p>");
    }

    // ------------------------------------------------------------------ SMS

    public static String bookingSmsToPatient(AppointmentInfo info) {
        return "Dear " + info.patientName() + ",\n"
                + "Your appointment has been successfully scheduled.\n"
                + "Doctor: " + info.doctorName() + "\n"
                + "Date: " + info.date() + "\n"
                + "Time: " + info.time() + "\n"
                + "Please reach on time.\n"
                + "- Hospital ERP";
    }

    public static String bookingSmsToDoctor(AppointmentInfo info) {
        return "New appointment scheduled.\n"
                + "Patient: " + info.patientName() + "\n"
                + "Date: " + info.date() + "\n"
                + "Time: " + info.time() + "\n"
                + "Please check the schedule.\n"
                + "- Hospital ERP";
    }

    public static String cancellationSmsToPatient(AppointmentInfo info) {
        return "Dear " + info.patientName() + ",\n"
                + "Your appointment has been cancelled.\n"
                + "Doctor: " + info.doctorName() + "\n"
                + "Date: " + info.date() + "\n"
                + "Time: " + info.time() + "\n"
                + "If this was a mistake, please rebook.\n"
                + "- Hospital ERP";
    }

    public static String cancellationSmsToDoctor(AppointmentInfo info) {
        return "Appointment Cancelled.\n"
                + "Patient: " + info.patientName() + "\n"
                + "Date: " + info.date() + "\n"
                + "Time: " + info.time() + "\n"
                + "Please update your schedule.\n"
                + "- Hospital ERP";
    }

    public static String leaveCancellationSmsToPatient(AppointmentInfo info) {
        return "Dear " + info.patientName() + ",\n"
                + "Your appointment with Dr. " + info.doctorName() + " on " + info.date()
                + " is cancelled due to doctor's leave.\n"
                + "Please reschedule your appointment by selecting another date.\n"
                + "- Hospital ERP";
    }

    public static String reminderSmsToPatient(AppointmentInfo info) {
        return "Dear " + info.patientName() + ",\n"
                + "Reminder: your appointment with Dr. " + info.doctorName() + " is tomorrow.\n"
                + "Date: " + info.date() + "\n"
                + "Time: " + info.time() + "\n"
                + "Please reach 10 minutes early.\n"
                + "- Hospital ERP";
    }

    // ------------------------------------------------------------------ helpers

    private static Email email(String subject, String body) {
        return new Email(subject, EmailService.layout(body));
    }

    // The person row (doctor or patient), then date and time
    private static String details(String personLabel, String person, AppointmentInfo info) {
        return "<table style='width:100%; font-size:14px;'>"
                + "<tr><td><strong>" + personLabel + ":</strong></td><td>" + esc(person) + "</td></tr>"
                + "<tr><td><strong>Date:</strong></td><td>" + esc(info.date()) + "</td></tr>"
                + "<tr><td><strong>Time:</strong></td><td>" + esc(info.time()) + "</td></tr>"
                + "</table>";
    }

    // User-supplied values (names) must never be inserted into HTML unescaped
    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
