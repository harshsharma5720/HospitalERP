package com.itmonteur.hospitalerp.identity;

import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ConflictException;
import com.itmonteur.hospitalerp.identity.internal.JWTService;
import com.itmonteur.hospitalerp.identity.internal.LoginAttemptService;
import com.itmonteur.hospitalerp.identity.internal.OtpService;
import com.itmonteur.hospitalerp.identity.internal.RefreshTokenService;
import com.itmonteur.hospitalerp.identity.internal.SessionEndReason;
import com.itmonteur.hospitalerp.identity.internal.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final LoginAttemptService loginAttemptService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final RefreshTokenService refreshTokenService;
    private final CurrentUserService currentUserService;

    /**
     * A login's two tokens (docs/SAFER_LOGIN_TOKENS_PLAN.md): the short access token (JWT) for the response body, and
     * the refresh token for the httpOnly cookie, with how long the cookie lives.
     */
    public record Session(String accessToken, String refreshToken, Duration refreshMaxAge) {
    }

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JWTService jwtService,
                       AuthenticationManager authenticationManager, OtpService otpService,
                       LoginAttemptService loginAttemptService, ApplicationEventPublisher eventPublisher,
                       Clock clock, RefreshTokenService refreshTokenService, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.otpService = otpService;
        this.loginAttemptService = loginAttemptService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.refreshTokenService = refreshTokenService;
        this.currentUserService = currentUserService;
    }

    @Value("${app.otp.required:true}")
    private boolean otpRequired;

    public boolean isOtpRequired() {
        return otpRequired;
    }

    /**
     * Public self-registration. Always creates a PATIENT, whatever role the client sends;
     * doctors, receptionists and admins are created by an admin via /api/admin/**.
     */
    @Transactional
    public Session register(RegisterRequestDTO request) {
        if (otpRequired && !otpService.isPhoneVerified(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number not verified. Please verify it with the OTP first.");
        }
        User savedUser = createUser(request, Role.PATIENT);
        if (otpRequired) {
            otpService.consumeVerification(request.getPhoneNumber());
        }
        // Logged in right away, so the user does not have to log in after registering
        return session(savedUser);
    }

    /**
     * Creates the user and publishes {@link UserRegisteredEvent}. The patients and staff modules
     * create the matching profile (patient / doctor / receptionist) in synchronous listeners,
     * inside this same transaction. Admin use + public registration.
     */
    @Transactional
    public User createUser(RegisterRequestDTO request, Role role) {
        logger.info("Creating {} account: {}", role, request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCreatedAt(LocalDateTime.now(clock));
        User savedUser = userRepository.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser));
        logger.info("User {} created with role {}", savedUser.getUsername(), savedUser.getRole());
        return savedUser;
    }

    /** Throws BadCredentialsException (→ 401) when the username/password is wrong. */
    public Session login(LoginRequestDTO request) {
        loginAttemptService.checkNotLocked(request.getUsername());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (DisabledException e) {
            // Right password, deactivated account: not a failed guess, so it doesn't count towards the lock
            logger.warn("Login refused for deactivated account {}", request.getUsername());
            throw e;
        } catch (AuthenticationException e) {
            loginAttemptService.loginFailed(request.getUsername());
            logger.warn("Failed login for user {}", request.getUsername());
            throw e;
        }
        loginAttemptService.loginSucceeded(request.getUsername());
        User user = this.userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found"));
        logger.info("User {} logged in", request.getUsername());
        return session(user);
    }

    /** A new access token and a replaced refresh token; empty when the refresh token is no good. */
    public Optional<Session> refresh(String refreshToken) {
        try {
            RefreshTokenService.Refreshed refreshed = refreshTokenService.refresh(refreshToken);
            User user = refreshed.user();
            return Optional.of(new Session(accessToken(user), refreshed.next().token(), refreshed.next().maxAge()));
        } catch (RefreshTokenService.InvalidRefreshTokenException e) {
            return Optional.empty();
        }
    }

    /** Ends the refresh token's session (logout on this device). */
    public void logout(String refreshToken) {
        refreshTokenService.end(refreshToken);
    }

    /** "Log out of all devices": every login of the current user ends, and its access tokens are refused at once. */
    @Transactional
    public void logoutEverywhere() {
        User user = currentUserService.getCurrentUser();
        refreshTokenService.endEverywhere(user, SessionEndReason.LOGOUT_ALL);
    }

    private Session session(User user) {
        RefreshTokenService.Issued refresh = refreshTokenService.start(user);
        return new Session(accessToken(user), refresh.token(), refresh.maxAge());
    }

    private String accessToken(User user) {
        return jwtService.generateToken(toUserDetails(user), user.getId(), user.getTokenVersion());
    }

    public static Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new BadRequestException("Role is required");
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown role: " + role);
        }
    }

    private static UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .build();
    }
}
