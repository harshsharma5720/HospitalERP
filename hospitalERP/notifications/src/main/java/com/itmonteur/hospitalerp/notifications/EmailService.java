package com.itmonteur.hospitalerp.notifications;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Email delivery over SMTP. Appointment mails are written by {@link AppointmentMessages} and sent through the
 * outbox ({@link #sendHtml}); the password-reset mail is sent directly - the user is waiting for the code.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String mailHost;

    public EmailService(JavaMailSender mailSender, @Value("${spring.mail.host:}") String mailHost) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
    }

    /** False when no SMTP host is configured: then nothing can be sent (the outbox marks such mails skipped). */
    public boolean isEnabled() {
        return mailHost != null && !mailHost.isBlank();
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
        sendHtml(toEmail, subject, layout(bodyHtml));
    }

    /** The common layout around a mail body: card, signature, footer. */
    public static String layout(String bodyHtml) {
        return "<div style='font-family: Arial, sans-serif; padding: 20px; border-radius: 10px; border:1px solid #ddd;'>"
                + bodyHtml
                + "<br/><p>Thank you,</p>"
                + "<h3 style='color:#1B4F72;'>Hospital ERP Team</h3>"
                + "<hr style='border-top:1px solid #ccc;'/>"
                + "<small style='color:#777;'>This is an automated email. Please do not reply.</small>"
                + "</div>";
    }

    /** Sends a finished HTML mail (layout included). Does nothing without an address. */
    public void sendHtml(String toEmail, String subject, String html) throws MessagingException {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    // User-supplied values (names, messages) must never be inserted into HTML unescaped
    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
