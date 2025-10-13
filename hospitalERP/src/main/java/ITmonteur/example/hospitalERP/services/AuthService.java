package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import ITmonteur.example.hospitalERP.repositories.ReceptionistRepository;
import ITmonteur.example.hospitalERP.repositories.UserRepository;
import ITmonteur.example.hospitalERP.dto.AuthResponseDTO;
import ITmonteur.example.hospitalERP.dto.LoginRequestDTO;
import ITmonteur.example.hospitalERP.dto.RegisterRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    private ReceptionistRepository receptionistRepository;

    public AuthResponseDTO register(RegisterRequestDTO request) {
        logger.info("Attempting to register user: {}", request.getUsername());
        // check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("Registration failed: Username {} already exists", request.getUsername());
            throw new RuntimeException("Username already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.valueOf(request.getRole().toUpperCase())); // convert string to enum
        User savedUser = userRepository.save(user);
        logger.info("User {} registered successfully with role {}", savedUser.getUsername(), savedUser.getRole());
        // Role-based entity creation
        if (savedUser.getRole() == Role.DOCTOR) {
            Doctor doctor = new Doctor();
//            doctor.setUsername(savedUser.getUsername()); // foreign key (username)
            doctor.setEmail(savedUser.getEmail());
            doctor.setUser(savedUser); // link back to User
            doctorRepository.save(doctor);
            logger.info("Doctor entity created for user {}", savedUser.getUsername());
        } else if (savedUser.getRole() == Role.PATIENT) {
            PtInfo patient = new PtInfo();
//            patient.setUsername(savedUser.getUsername()); // foreign key (username)
            patient.setEmail(savedUser.getEmail());
            patient.setUser(savedUser);
            ptInfoRepository.save(patient);
            logger.info("Patient entity created for user {}", savedUser.getUsername());
        } else if (savedUser.getRole() == Role.RECEPTIONIST) {
            Receptionist receptionist = new Receptionist();
            receptionist.setEmail(savedUser.getEmail());
            receptionist.setUser(savedUser);
            receptionistRepository.save(receptionist);
            logger.info("Receptionist entity created for user {}", savedUser.getUsername());
        }
        // ye mne is liye kiya h taki register krne ke bad turant ek token generate ho jae bina login kre
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(savedUser.getUsername())   // username from DB
                .password(savedUser.getPassword())       // encoded password
                .authorities(new SimpleGrantedAuthority("ROLE_" + savedUser.getRole().name())) // map role
                .build();
        // 4. Generate JWT token for the saved user
        String token = jwtService.generateToken(userDetails, savedUser.getId());
        logger.info("JWT token generated for user {}", savedUser.getUsername());

        // 5. Return token in response object
        return new AuthResponseDTO(token);
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        logger.info("Attempting login for user: {}", request.getUsername());

        // authenticate user credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        // fetch user from DB
        User user = this.userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    logger.warn("Login failed: User {} not found", request.getUsername());
                    return new RuntimeException("User not found!");
                });

        Long userId = user.getId();
        // check authentication
        if (authentication.isAuthenticated()) {
            // generate JWT token for logged-in user
            String token = jwtService.generateToken((org.springframework.security.core.userdetails.User) authentication.getPrincipal(), userId);
            logger.info("User {} logged in successfully", request.getUsername());
            return new AuthResponseDTO(token);
        } else {
            logger.warn("Login failed: Invalid credentials for user {}", request.getUsername());
            throw new RuntimeException("Invalid credentials");
        }
    }
}
