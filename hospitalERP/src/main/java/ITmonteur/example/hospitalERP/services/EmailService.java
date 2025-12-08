package ITmonteur.example.hospitalERP.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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
                + "<p>Dear <strong>" + patientName + "</strong>,</p>"
                + "<p>Your appointment has been successfully booked with the following details:</p>"
                + "<table style='width:100%; font-size:14px;'>"
                + "<tr><td><strong>Doctor:</strong></td><td>" + doctorName + "</td></tr>"
                + "<tr><td><strong>Date:</strong></td><td>" + date + "</td></tr>"
                + "<tr><td><strong>Time:</strong></td><td>" + time + "</td></tr>"
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
                        + "<p>Dear <strong>" + patientName + "</strong>,</p>"
                        + "<p>Your appointment has been cancelled.</p>"
                        + "<table style='width:100%; font-size:14px;'>"
                        + "<tr><td><strong>Doctor:</strong></td><td>" + doctorName + "</td></tr>"
                        + "<tr><td><strong>Date:</strong></td><td>" + date + "</td></tr>"
                        + "<tr><td><strong>Time:</strong></td><td>" + time + "</td></tr>"
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
                        + "<p>Dear <strong>Dr. " + doctorName + "</strong>,</p>"
                        + "<p>A new appointment has been scheduled with the following patient:</p>"
                        + "<table style='width:100%; font-size:14px;'>"
                        + "<tr><td><strong>Patient Name:</strong></td><td>" + patientName + "</td></tr>"
                        + "<tr><td><strong>Date:</strong></td><td>" + date + "</td></tr>"
                        + "<tr><td><strong>Time:</strong></td><td>" + time + "</td></tr>"
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
                        + "<p>Dear <strong>" + patientName + "</strong>,</p>"
                        + "<p>Your appointment with <strong>Dr. " + doctorName + "</strong> has been cancelled due to doctor's leave.</p>"
                        + "<table style='width:100%; font-size:14px;'>"
                        + "<tr><td><strong>Date:</strong></td><td>" + date + "</td></tr>"
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


}
