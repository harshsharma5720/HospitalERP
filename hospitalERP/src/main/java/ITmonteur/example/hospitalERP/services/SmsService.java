package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.configuration.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    private final TwilioConfig twilioConfig;

    public SmsService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    public boolean isEnabled() {
        return twilioConfig.isConfigured();
    }

    /** Sends an SMS. Skipped (with a warning) when Twilio is not configured or the number is empty. */
    public void send(String phoneNumber, String messageBody) {
        if (!isEnabled()) {
            logger.warn("Twilio is not configured - SMS to {} was not sent", mask(phoneNumber));
            return;
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            logger.warn("No phone number - SMS was not sent");
            return;
        }
        Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioConfig.getTrialNumber()),
                messageBody
        ).create();
    }

    public void sendOtp(String phoneNumber, String otp, int validMinutes) {
        send(phoneNumber, "Your Hospital ERP OTP is: " + otp
                + "\nThis OTP is valid for " + validMinutes + " minutes."
                + "\nDo not share it with anyone.");
    }

    public void sendAppointmentSms(String phoneNumber, String patientName, String doctorName, String date, String time) {
        send(phoneNumber, "Dear " + patientName + ",\n"
                + "Your appointment has been successfully scheduled.\n"
                + "Doctor: " + doctorName + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Please reach on time.\n"
                + "- Hospital ERP");
    }

    public void sendDoctorSms(String phoneNumber, String patientName, String doctorName, String date, String time) {
        send(phoneNumber, "New appointment scheduled.\n"
                + "Patient: " + patientName + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Please check the schedule.\n"
                + "- Hospital ERP");
    }

    public void sendAppointmentCancelSms(String phoneNumber, String patientName, String doctorName, String date, String time) {
        send(phoneNumber, "Dear " + patientName + ",\n"
                + "Your appointment has been cancelled.\n"
                + "Doctor: " + doctorName + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "If this was a mistake, please rebook.\n"
                + "- Hospital ERP");
    }

    public void sendDoctorCancelSms(String phoneNumber, String patientName, String doctorName, String date, String time) {
        send(phoneNumber, "Appointment Cancelled.\n"
                + "Patient: " + patientName + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Please update your schedule.\n"
                + "- Hospital ERP");
    }

    public void sendDoctorLeaveCancelSms(String phoneNumber, String patientName, String doctorName, String date) {
        send(phoneNumber, "Dear " + patientName + ",\n"
                + "Your appointment with Dr. " + doctorName + " on " + date + " is cancelled due to doctor's leave.\n"
                + "Please reschedule your appointment by selecting another date.\n"
                + "- Hospital ERP");
    }

    public void sendReminderSms(String phoneNumber, String patientName, String doctorName, String date, String time) {
        send(phoneNumber, "Dear " + patientName + ",\n"
                + "Reminder: your appointment with Dr. " + doctorName + " is tomorrow.\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Please reach 10 minutes early.\n"
                + "- Hospital ERP");
    }

    public void sendPasswordResetSms(String phoneNumber, String otp, int validMinutes) {
        send(phoneNumber, "Your Hospital ERP password reset code is: " + otp
                + "\nValid for " + validMinutes + " minutes."
                + "\nIf you didn't request this, ignore this message.");
    }

    static String mask(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
