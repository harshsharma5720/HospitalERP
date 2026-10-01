package com.itmonteur.hospitalerp.identity;

import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ConflictException;
import com.itmonteur.hospitalerp.identity.internal.UserRepository;
import com.itmonteur.hospitalerp.identity.internal.JWTService;
import com.itmonteur.hospitalerp.identity.internal.LoginAttemptService;
import com.itmonteur.hospitalerp.identity.internal.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JWTService jwtService,
                       AuthenticationManager authenticationManager, OtpService otpService,
                       LoginAttemptService loginAttemptService, ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.otpService = otpService;
        this.loginAttemptService = loginAttemptService;
        this.eventPublisher = eventPublisher;
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
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (otpRequired && !otpService.isPhoneVerified(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number not verified. Please verify it with the OTP first.");
        }
        User savedUser = createUser(request, Role.PATIENT);
        if (otpRequired) {
            otpService.consumeVerification(request.getPhoneNumber());
        }
        // Issue a token right away so the user does not have to log in after registering
        return new AuthResponseDTO(jwtService.generateToken(toUserDetails(savedUser), savedUser.getId()));
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
        User savedUser = userRepository.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser));
        logger.info("User {} created with role {}", savedUser.getUsername(), savedUser.getRole());
        return savedUser;
    }

    /** Throws BadCredentialsException (→ 401) when the username/password is wrong. */
    public AuthResponseDTO login(LoginRequestDTO request) {
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
        String token = jwtService.generateToken((UserDetails) authentication.getPrincipal(), user.getId());
        logger.info("User {} logged in", request.getUsername());
        return new AuthResponseDTO(token);
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
