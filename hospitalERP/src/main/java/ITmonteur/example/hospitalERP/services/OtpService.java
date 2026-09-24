package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phone verification with one-time passwords.
 * OTPs are random (SecureRandom), stored only as a hash, expire after 5 minutes,
 * allow a limited number of attempts, and can be re-sent at most once per minute.
 * State is kept in memory, so it is lost on restart (acceptable for a single instance).
 */
@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    static final Duration OTP_VALIDITY = Duration.ofMinutes(5);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final Duration VERIFIED_VALIDITY = Duration.ofMinutes(30);
    static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> pendingOtps = new ConcurrentHashMap<>();
    private final Map<String, Instant> verifiedPhones = new ConcurrentHashMap<>();
    private final SmsService smsService;

    private record OtpEntry(String hash, Instant expiresAt, Instant sentAt, int attempts) {}

    public OtpService(SmsService smsService) {
        this.smsService = smsService;
    }

    public void sendOtp(String rawPhone) {
        String phone = normalize(rawPhone);
        if (!smsService.isEnabled()) {
            throw new IllegalStateException("SMS service is not configured");
        }
        Instant now = Instant.now();
        OtpEntry existing = pendingOtps.get(phone);
        if (existing != null && existing.sentAt().plus(RESEND_COOLDOWN).isAfter(now)) {
            throw new ConflictException("Please wait a minute before requesting another OTP");
        }
        String otp = String.format("%06d", random.nextInt(1_000_000));
        pendingOtps.put(phone, new OtpEntry(hash(otp), now.plus(OTP_VALIDITY), now, 0));
        smsService.sendOtp(phone, otp, (int) OTP_VALIDITY.toMinutes());
        logger.info("OTP sent to {}", SmsService.mask(phone));
    }

    public boolean verifyOtp(String rawPhone, String enteredOtp) {
        String phone = normalize(rawPhone);
        OtpEntry entry = pendingOtps.get(phone);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            pendingOtps.remove(phone);
            return false;
        }
        if (enteredOtp != null && MessageDigest.isEqual(
                entry.hash().getBytes(StandardCharsets.UTF_8),
                hash(enteredOtp.trim()).getBytes(StandardCharsets.UTF_8))) {
            pendingOtps.remove(phone);
            verifiedPhones.put(phone, Instant.now().plus(VERIFIED_VALIDITY));
            return true;
        }
        int attempts = entry.attempts() + 1;
        if (attempts >= MAX_ATTEMPTS) {
            pendingOtps.remove(phone);
            logger.warn("Too many wrong OTP attempts for {}", SmsService.mask(phone));
        } else {
            pendingOtps.put(phone, new OtpEntry(entry.hash(), entry.expiresAt(), entry.sentAt(), attempts));
        }
        return false;
    }

    public boolean isPhoneVerified(String rawPhone) {
        Instant validUntil = verifiedPhones.get(normalize(rawPhone));
        return validUntil != null && validUntil.isAfter(Instant.now());
    }

    // A verification is used up by one successful registration
    public void consumeVerification(String rawPhone) {
        verifiedPhones.remove(normalize(rawPhone));
    }

    private static String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Phone number is required");
        }
        return phone.replaceAll("[\\s-]", "");
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
