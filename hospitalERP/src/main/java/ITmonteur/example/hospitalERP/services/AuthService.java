package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ConflictException;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import ITmonteur.example.hospitalERP.repositories.ReceptionistRepository;
import ITmonteur.example.hospitalERP.repositories.UserRepository;
import ITmonteur.example.hospitalERP.dto.AuthResponseDTO;
import ITmonteur.example.hospitalERP.dto.LoginRequestDTO;
import ITmonteur.example.hospitalERP.dto.RegisterRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
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

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JWTService jwtService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private PtInfoRepository ptInfoRepository;
    @Autowired
    private OtpService otpService;
    @Autowired
    private ReceptionistRepository receptionistRepository;
    @Autowired
    private LoginAttemptService loginAttemptService;

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

    /** Creates the user plus its role profile (doctor / patient / receptionist). Admin use only. */
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
        // Role-based entity creation
        switch (savedUser.getRole()) {
            case DOCTOR:
                Doctor doctor = new Doctor();
                doctor.setEmail(savedUser.getEmail());
                doctor.setUserName(savedUser.getUsername()); // foreign key (username)
                doctor.setName(savedUser.getUsername());
                doctor.setSpecialist(Specialist.NOT_ASSIGNED);
                doctor.setPhoneNumber(savedUser.getPhoneNumber());
                doctor.setUser(savedUser);
                doctorRepository.save(doctor);
                break;
            case PATIENT:
                PtInfo patient = new PtInfo();
                patient.setEmail(savedUser.getEmail());
                patient.setUserName(savedUser.getUsername());
                patient.setUser(savedUser);
                patient.setPatientName(savedUser.getUsername());
                patient.setPatientAddress("Not provided");
                patient.setContactNo(savedUser.getPhoneNumber());
                patient.setPatientAadharNo(null);
                patient.setGender(Gender.OTHER);
                patient.setDob(null); // asked for on the profile page instead of a fake date
                ptInfoRepository.save(patient);
                break;
            case RECEPTIONIST:
                Receptionist receptionist = new Receptionist();
                receptionist.setEmail(savedUser.getEmail());
                receptionist.setUserName(savedUser.getUsername());
                receptionist.setName(savedUser.getUsername());
                receptionist.setPhone(savedUser.getPhoneNumber());
                receptionist.setUser(savedUser);
                receptionist.setGender(Gender.OTHER);
                receptionistRepository.save(receptionist);
                break;
            default:
                // ADMIN has no profile entity
                break;
        }
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
