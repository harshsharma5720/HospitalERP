package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic brute-force protection: after MAX_FAILURES wrong passwords for a username,
 * further logins for it are refused for LOCK_DURATION. In-memory, per instance.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record Attempts(int failures, Instant firstFailure) {}

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public void checkNotLocked(String username) {
        Attempts entry = attempts.get(key(username));
        if (entry == null) {
            return;
        }
        if (entry.firstFailure().plus(LOCK_DURATION).isBefore(Instant.now())) {
            attempts.remove(key(username));
            return;
        }
        if (entry.failures() >= MAX_FAILURES) {
            throw new TooManyRequestsException("Too many failed login attempts. Please try again in 15 minutes.");
        }
    }

    public void loginFailed(String username) {
        attempts.merge(key(username), new Attempts(1, Instant.now()),
                (old, ignored) -> new Attempts(old.failures() + 1, old.firstFailure()));
    }

    public void loginSucceeded(String username) {
        attempts.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
