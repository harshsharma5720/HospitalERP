package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.User;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ConflictException;
import ITmonteur.example.hospitalERP.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * "Forgot password": a one-time code is sent to the account's phone (SMS) and email,
 * and the code lets the user choose a new password.
 * Responses never reveal whether an account exists.
 */
@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    static final String INVALID_CODE = "Invalid or expired code";

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final SmsService smsService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public PasswordResetService(UserRepository userRepository, OtpService otpService, SmsService smsService,
                                EmailService emailService, PasswordEncoder passwordEncoder,
                                LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.smsService = smsService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    /** Sends a reset code if the username/email exists; silently does nothing otherwise. */
    public void requestReset(String identifier) {
        Optional<User> found = findUser(identifier);
        if (found.isEmpty()) {
            logger.info("Password reset requested for unknown account");
            return;
        }
        User user = found.get();
        String otp;
        try {
            otp = otpService.issue(key(user));
        } catch (ConflictException tooSoon) {
            return; // a code was sent less than a minute ago; answer the same way
        }
        int minutes = otpService.validityMinutes();
        boolean delivered = false;
        if (smsService.isEnabled() && user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            try {
                smsService.sendPasswordResetSms(user.getPhoneNumber(), otp, minutes);
                delivered = true;
            } catch (Exception e) {
                logger.warn("Could not send reset SMS: {}", e.getMessage());
            }
        }
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), otp, minutes);
            delivered = true;
        } catch (Exception e) {
            logger.warn("Could not send reset email: {}", e.getMessage());
        }
        if (!delivered) {
            logger.warn("Password reset code for user {} could not be delivered (no SMS or email configured)", user.getId());
        } else {
            logger.info("Password reset code sent for user {}", user.getId());
        }
    }

    /** Sets a new password when the code is valid. Also lifts any failed-login lock. */
    public void resetPassword(String identifier, String code, String newPassword) {
        User user = findUser(identifier).orElseThrow(() -> new BadRequestException(INVALID_CODE));
        if (!otpService.checkCode(key(user), code)) {
            throw new BadRequestException(INVALID_CODE);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        loginAttemptService.loginSucceeded(user.getUsername());
        logger.info("Password reset completed for user {}", user.getId());
    }

    private Optional<User> findUser(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String value = identifier.trim();
        Optional<User> byUsername = userRepository.findByUsername(value);
        return byUsername.isPresent() ? byUsername : userRepository.findByEmail(value);
    }

    private static String key(User user) {
        return "reset:" + user.getId();
    }
}
