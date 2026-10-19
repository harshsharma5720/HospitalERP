package com.itmonteur.hospitalerp.identity.internal;

import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Login sessions with refresh tokens (docs/SAFER_LOGIN_TOKENS_PLAN.md). A token is 256 random bits, kept only as its
 * SHA-256 hash, and replaced whenever it is used. An old token used again more than {@link #REUSE_GRACE} after it was
 * replaced means it was copied: the whole session ends. Within the grace it is two tabs refreshing at once.
 * Patients: 7 days after the last use, at most 90 days after the login; staff: 12 hours after the login.
 */
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    public static final Duration REUSE_GRACE = Duration.ofSeconds(30);

    /** A new refresh token and how long its cookie should live. */
    public record Issued(String token, Duration maxAge) {
    }

    /** A used refresh token's user and its replacement. */
    public record Refreshed(User user, Issued next) {
    }

    /** The token is missing, unknown, used up, expired, or its session ended. */
    public static class InvalidRefreshTokenException extends RuntimeException {
        InvalidRefreshTokenException() {
            super("Your session has ended. Please log in again.");
        }
    }

    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final Clock clock;
    private final Duration patientIdle;
    private final Duration patientMax;
    private final Duration staffMax;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository tokens, UserRepository users, Clock clock,
                               @Value("${app.auth.patient-idle-days:7}") long patientIdleDays,
                               @Value("${app.auth.patient-max-days:90}") long patientMaxDays,
                               @Value("${app.auth.staff-hours:12}") long staffHours) {
        this.tokens = tokens;
        this.users = users;
        this.clock = clock;
        this.patientIdle = Duration.ofDays(patientIdleDays);
        this.patientMax = Duration.ofDays(patientMaxDays);
        this.staffMax = Duration.ofHours(staffHours);
    }

    /** A new session for a login. */
    @Transactional
    public Issued start(User user) {
        LocalDateTime now = now();
        LocalDateTime sessionEnd = now.plus(user.getRole() == Role.PATIENT ? patientMax : staffMax);
        return issue(user, UUID.randomUUID().toString(), now, sessionEnd);
    }

    /** Uses a refresh token: its user and the token that replaces it. Ending a session is kept when this throws. */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Refreshed refresh(String presented) {
        if (presented == null || presented.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken token = tokens.findByTokenHashForUpdate(hash(presented)).orElseThrow(InvalidRefreshTokenException::new);
        LocalDateTime now = now();
        if (token.getEndedAt() != null) {
            throw new InvalidRefreshTokenException();
        }
        if (token.getReplacedAt() != null && token.getReplacedAt().plus(REUSE_GRACE).isBefore(now)) {
            logger.warn("A replaced refresh token was used again: session {} of user {} ended", token.getSessionId(),
                    token.getUserId());
            endSession(token.getSessionId(), SessionEndReason.REUSED, now);
            throw new InvalidRefreshTokenException();
        }
        if (!now.isBefore(token.getExpiresAt())) {
            throw new InvalidRefreshTokenException();
        }
        User user = users.findById(token.getUserId()).orElse(null);
        if (user == null || !user.isActive()) {
            endSession(token.getSessionId(), SessionEndReason.DEACTIVATED, now);
            throw new InvalidRefreshTokenException();
        }
        if (token.getReplacedAt() == null) {
            token.setReplacedAt(now); // within the grace a second tab gets its own replacement too
        }
        return new Refreshed(user, issue(user, token.getSessionId(), now, token.getSessionExpiresAt()));
    }

    /** Logout: the token's session ends (nothing happens for an unknown token). */
    @Transactional
    public void end(String presented) {
        if (presented == null || presented.isBlank()) {
            return;
        }
        tokens.findByTokenHash(hash(presented)).ifPresent(token -> endSession(token.getSessionId(), SessionEndReason.LOGOUT, now()));
    }

    /** Every session of the user ends (log out everywhere, a password reset, a deactivation). */
    @Transactional
    public void endAll(long userId, SessionEndReason reason) {
        LocalDateTime now = now();
        tokens.findByUserIdAndEndedAtIsNull(userId).forEach(token -> token.end(now, reason));
    }

    private void endSession(String sessionId, SessionEndReason reason, LocalDateTime now) {
        tokens.findBySessionIdAndEndedAtIsNull(sessionId).forEach(token -> token.end(now, reason));
    }

    private Issued issue(User user, String sessionId, LocalDateTime now, LocalDateTime sessionEnd) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime candidate = user.getRole() == Role.PATIENT ? now.plus(patientIdle) : sessionEnd;
        LocalDateTime expires = candidate.isAfter(sessionEnd) ? sessionEnd : candidate;
        tokens.save(new RefreshToken(user.getId(), sessionId, hash(token), now, expires, sessionEnd));
        return new Issued(token, Duration.between(now, expires));
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
