package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.configuration.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class SmsService {

    private final TwilioConfig twilioConfig;

    private final Map<String, String> otpStore = new HashMap<>();
    private final Map<String, Boolean> verifiedPhones = new HashMap<>();

    public SmsService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }


    public void sendOtp(String phoneNumber) {

        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStore.put(phoneNumber, otp);

        String messageBody = "Your Hospital ERP OTP is: " + otp
                + "\nThis OTP is valid for 5 minutes."
                + "\nDo not share it with anyone.";

        Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioConfig.getTrialNumber()),
                messageBody
        ).create();
    }
    public boolean verifyOtp(String phoneNumber, String enteredOtp) {

        String storedOtp = otpStore.get(phoneNumber);

        if (storedOtp != null && storedOtp.equals(enteredOtp)) {
            otpStore.remove(phoneNumber);
            verifiedPhones.put(phoneNumber, true);
            return true;
        }
        return false;
    }

    public boolean isPhoneVerified(String phoneNumber) {
        return verifiedPhones.getOrDefault(phoneNumber, false);
    }

    public void sendAppointmentSms(String phoneNumber, String patientName, String doctorName, String date, String time) {

        String messageBody = "Dear " + patientName + ",\n"
                + "Your appointment has been successfully scheduled.\n"
                + "Doctor: " + doctorName + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Please reach on time.\n"
                + "- Hospital ERP";

        Message.creator(
                new PhoneNumber(phoneNumber),                // recipient number
                new PhoneNumber(twilioConfig.getTrialNumber()), // your twilio number
                messageBody                                   // auto formatted message
        ).create();
    }

    public void sendDoctorSms(String phoneNumber, String patientName, String doctorName, String date, String time) {

        String messageBody = "New appointment scheduled.\n"
                + "Patient: " + patientName + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Please check the schedule.\n"
                + "- Hospital ERP";

        Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioConfig.getTrialNumber()),
                messageBody
        ).create();
    }

    public void sendAppointmentCancelSms(String phoneNumber, String patientName, String doctorName, String date, String time) {

        String messageBody = "Dear " + patientName + ",\n"
                + "Your appointment has been cancelled.\n"
                + "Doctor: " + doctorName + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "If this was a mistake, please rebook.\n"
                + "- Hospital ERP";

        Message.creator(
                new PhoneNumber(phoneNumber),                // recipient mobile number
                new PhoneNumber(twilioConfig.getTrialNumber()), // your Twilio registered number
                messageBody
        ).create();
    }

    public void sendDoctorCancelSms(String phoneNumber, String patientName, String doctorName, String date, String time) {

        String messageBody = "Appointment Cancelled.\n"
                + "Patient: " + patientName + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Please update your schedule.\n"
                + "- Hospital ERP";

        Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioConfig.getTrialNumber()),
                messageBody
        ).create();
    }
    public void sendDoctorLeaveCancelSms(String phoneNumber, String patientName, String doctorName, String date) {

        String messageBody = "Dear " + patientName + ",\n"
                + "Your appointment with Dr. " + doctorName + " on " + date + " is cancelled due to doctor's leave.\n"
                + "Please reschedule your appointment by selecting another date.\n"
                + "- Hospital ERP";

        Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioConfig.getTrialNumber()),
                messageBody
        ).create();
    }

}
