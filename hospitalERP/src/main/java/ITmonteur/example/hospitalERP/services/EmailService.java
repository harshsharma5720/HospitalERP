package ITmonteur.example.hospitalERP.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBookingEmail(String toEmail, String patientName, String doctorName, String date, String time) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Appointment Confirmation");

        message.setText(
                "Dear " + patientName + ",\n\n" +
                        "Your appointment has been successfully booked.\n" +
                        "Doctor: " + doctorName + "\n" +
                        "Date: " + date + "\n" +
                        "Time: " + time + "\n\n" +
                        "Please reach on time.\n" +
                        "Thank you."
        );

        mailSender.send(message);
    }
}
