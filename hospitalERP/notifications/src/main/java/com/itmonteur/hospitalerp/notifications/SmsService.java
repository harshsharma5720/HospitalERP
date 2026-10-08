package com.itmonteur.hospitalerp.notifications;

import com.itmonteur.hospitalerp.notifications.internal.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SMS delivery over Twilio. Appointment texts are written by {@link AppointmentMessages} and sent through the
 * outbox ({@link #send}); OTP and password-reset codes are sent directly - the user is waiting for them.
 */
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

    public void sendPasswordResetSms(String phoneNumber, String otp, int validMinutes) {
        send(phoneNumber, "Your Hospital ERP password reset code is: " + otp
                + "\nValid for " + validMinutes + " minutes."
                + "\nIf you didn't request this, ignore this message.");
    }

    public static String mask(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
