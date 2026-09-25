package ITmonteur.example.hospitalERP.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBookingEmail(String toEmail, String patientName, String doctorName, String date, String time) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(toEmail);
        helper.setSubject("Appointment Confirmation - Hospital ERP");

        String htmlContent = "</br>"
                + "<div style='font-family: Arial, sans-serif; padding: 20px; border-radius: 10px; border:1px solid #ddd;'>"
                + "<h2 style='color:#2E86C1;'>Appointment Confirmed ✔</h2>"
                + "<p>Dear <strong>" + esc(patientName) + "</strong>,</p>"
                + "<p>Your appointment has been successfully booked with the following details:</p>"
                + "<table style='width:100%; font-size:14px;'>"
                + "<tr><td><strong>Doctor:</strong></td><td>" + esc(doctorName) + "</td></tr>"
                + "<tr><td><strong>Date:</strong></td><td>" + esc(date) + "</td></tr>"
                + "<tr><td><strong>Time:</strong></td><td>" + esc(time) + "</td></tr>"
                + "</table>"
                + "<br/>"
                + "<p style='margin-top:10px;'>Please reach the hospital <strong>10 minutes before</strong> the scheduled time.</p>"
                + "<br/>"
                + "<p>Thank you,</p>"
                + "<h3 style='color:#1B4F72;'>Hospital ERP Team</h3>"
                + "<hr style='border-top:1px solid #ccc;'/>"
                + "<small style='color:#777;'>This is an automated email. Please do not reply.</small>"
                + "</div>";

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    public void sendDeleteEmail(String toEmail, String patientName, String doctorName, String date, String time) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(toEmail);
        helper.setSubject("Appointment Cancelled - Hospital ERP");

        String htmlContent =
                "<div style='font-family: Arial, sans-serif; padding: 20px; border-radius:10px; border:1px solid #ddd;'>"
                        + "<h2 style='color:#C0392B;'>Appointment Cancelled ❌</h2>"
                        + "<p>Dear <strong>" + esc(patientName) + "</strong>,</p>"
                        + "<p>Your appointment has been cancelled.</p>"
                        + "<table style='width:100%; font-size:14px;'>"
                        + "<tr><td><strong>Doctor:</strong></td><td>" + esc(doctorName) + "</td></tr>"
                        + "<tr><td><strong>Date:</strong></td><td>" + esc(date) + "</td></tr>"
                        + "<tr><td><strong>Time:</strong></td><td>" + esc(time) + "</td></tr>"
                        + "</table>"
                        + "<br/>"
                        + "<p>If this was a mistake, please book again from the portal.</p>"
                        + "<br/>"
                        + "<p>Thank you,</p>"
                        + "<h3 style='color:#1B4F72;'>Hospital ERP Team</h3>"
                        + "<hr/>"
                        + "<small style='color:#777;'>This is an automated email. Please do not reply.</small>"
                        + "</div>";

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    public void sendDoctorNotificationEmail(String toEmail, String patientName, String doctorName, String date, String time) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(toEmail);
        helper.setSubject("New Appointment Scheduled - Hospital ERP");

        String htmlContent =
                "<div style='font-family: Arial, sans-serif; padding: 20px; border-radius: 10px; border:1px solid #ddd;'>"
                        + "<h2 style='color:#2E86C1;'>New Appointment Scheduled 📢</h2>"
                        + "<p>Dear <strong>Dr. " + esc(doctorName) + "</strong>,</p>"
                        + "<p>A new appointment has been scheduled with the following patient:</p>"
                        + "<table style='width:100%; font-size:14px;'>"
                        + "<tr><td><strong>Patient Name:</strong></td><td>" + esc(patientName) + "</td></tr>"
                        + "<tr><td><strong>Date:</strong></td><td>" + esc(date) + "</td></tr>"
                        + "<tr><td><strong>Time:</strong></td><td>" + esc(time) + "</td></tr>"
                        + "</table>"
                        + "<br/>"
                        + "<p>Please review the appointment details and be prepared accordingly.</p>"
                        + "<br/>"
                        + "<p>Thank you,</p>"
                        + "<h3 style='color:#1B4F72;'>Hospital ERP Team</h3>"
                        + "<hr style='border-top:1px solid #ccc;'/>"
                        + "<small style='color:#777;'>This is an automated email. Please do not reply.</small>"
                        + "</div>";

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    public void sendDoctorLeaveCancelEmail(String toEmail, String patientName, String doctorName, String date) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(toEmail);
        helper.setSubject("Appointment Cancelled Due to Doctor Leave - Hospital ERP");

        String htmlContent =
                "<div style='font-family: Arial, sans-serif; padding: 20px; border-radius:10px; border:1px solid #ddd;'>"
                        + "<h2 style='color:#C0392B;'>Appointment Cancelled ❌</h2>"
                        + "<p>Dear <strong>" + esc(patientName) + "</strong>,</p>"
                        + "<p>Your appointment with <strong>Dr. " + esc(doctorName) + "</strong> has been cancelled due to doctor's leave.</p>"
                        + "<table style='width:100%; font-size:14px;'>"
                        + "<tr><td><strong>Date:</strong></td><td>" + esc(date) + "</td></tr>"
                        + "</table>"
                        + "<br/>"
                        + "<p>Please <strong>rebook</strong> your appointment from the portal.</p>"
                        + "<br/>"
                        + "<p>Thank you,</p>"
                        + "<h3 style='color:#1B4F72;'>Hospital ERP Team</h3>"
                        + "<hr style='border-top:1px solid #ccc;'/>"
                        + "<small style='color:#777;'>This is an automated email. Please do not reply.</small>"
                        + "</div>";

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    public void sendReminderEmail(String toEmail, String patientName, String doctorName, String date, String time) throws MessagingException {
        send(toEmail, "Appointment Reminder - Hospital ERP",
                "<h2 style='color:#2E86C1;'>Appointment Reminder ⏰</h2>"
                        + "<p>Dear <strong>" + esc(patientName) + "</strong>,</p>"
                        + "<p>This is a reminder of your appointment <strong>tomorrow</strong>:</p>"
                        + "<table style='width:100%; font-size:14px;'>"
                        + "<tr><td><strong>Doctor:</strong></td><td>Dr. " + esc(doctorName) + "</td></tr>"
                        + "<tr><td><strong>Date:</strong></td><td>" + esc(date) + "</td></tr>"
                        + "<tr><td><strong>Time:</strong></td><td>" + esc(time) + "</td></tr>"
                        + "</table>"
                        + "<p style='margin-top:10px;'>Please reach the hospital <strong>10 minutes before</strong> the scheduled time. "
                        + "If you can't make it, please cancel or reschedule from the portal.</p>");
    }

    public void sendPasswordResetEmail(String toEmail, String username, String otp, int validMinutes) throws MessagingException {
        send(toEmail, "Password Reset Code - Hospital ERP",
                "<h2 style='color:#2E86C1;'>Password Reset</h2>"
                        + "<p>Hello <strong>" + esc(username) + "</strong>,</p>"
                        + "<p>Your password reset code is:</p>"
                        + "<p style='font-size:28px; font-weight:bold; letter-spacing:6px;'>" + esc(otp) + "</p>"
                        + "<p>It is valid for " + validMinutes + " minutes. If you did not request a password reset, "
                        + "you can ignore this email — your password will not change.</p>");
    }

    // Wraps the body in the common layout (card + signature + footer)
    private void send(String toEmail, String subject, String bodyHtml) throws MessagingException {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText("<div style='font-family: Arial, sans-serif; padding: 20px; border-radius: 10px; border:1px solid #ddd;'>"
                + bodyHtml
                + "<br/><p>Thank you,</p>"
                + "<h3 style='color:#1B4F72;'>Hospital ERP Team</h3>"
                + "<hr style='border-top:1px solid #ccc;'/>"
                + "<small style='color:#777;'>This is an automated email. Please do not reply.</small>"
                + "</div>", true);
        mailSender.send(message);
    }

    // User-supplied values (names, messages) must never be inserted into HTML unescaped
    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
