package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.controller.AuthController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OTPService {

    private static final Logger logger = LoggerFactory.getLogger(OTPService.class);

    private final Map<String, String> otpStore = new HashMap<>();
    //  Keep track of which numbers are verified
    private Map<String, Boolean> verifiedPhones = new HashMap<>();

    // Generate and send OTP
    public void generateAndSendOTP(String phoneNumber) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStore.put(phoneNumber, otp);
        System.out.println("Generated OTP for " + phoneNumber + " : " + otp);
        // In real apps — send this via Twilio, MSG91, or Firebase SMS API
    }

    // Verify OTP
    public boolean verifyOTP(String phoneNumber, String enteredOtp) {
        String storedOtp = otpStore.get(phoneNumber);
        logger.info("Stored OTP : {}", storedOtp);
        logger.info("Entered OTP : {}", enteredOtp);
        if (storedOtp != null && storedOtp.equals(enteredOtp)) {
            otpStore.remove(phoneNumber);
            verifiedPhones.put(phoneNumber, true);
            return true;
        }
        return false;
    }


    public boolean isPhoneVerified(String phoneNumber) {
        return verifiedPhones.get(phoneNumber);
    }
}
