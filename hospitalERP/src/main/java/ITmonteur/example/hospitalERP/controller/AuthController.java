package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.AuthResponseDTO;
import ITmonteur.example.hospitalERP.dto.LoginRequestDTO;
import ITmonteur.example.hospitalERP.dto.RegisterRequestDTO;
import ITmonteur.example.hospitalERP.services.AuthService;
import ITmonteur.example.hospitalERP.services.JWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private JWTService jwtService;

    // Register a new user (Doctor, Patient, Receptionist)
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@RequestBody RegisterRequestDTO request) {
        logger.info("Register request received for username: {}", request.getUsername());
        AuthResponseDTO response = authService.register(request);
        logger.info("User registered successfully: {} | Role: {}", request.getUsername(), request.getRole());
        return ResponseEntity.ok(response);
    }

    // User login
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO request) {
        logger.info("Login request received for username: {}", request.getUsername());
        AuthResponseDTO response = authService.login(request);
        logger.info("User logged in successfully: {}", request.getUsername());
        return ResponseEntity.ok(response);
    }

//    @GetMapping("/role")
//    public ResponseEntity<String> getUserRole(@RequestHeader("Authorization") String authHeader) {
//        logger.info("Received request to fetch user role.");
//        try {
//            String token = authHeader.substring(7);// Extract token
//            logger.debug("Extracted JWT Token: {}", token);
//            String role = jwtService.extractUserRole(token);// Extracting the decoded role
//            logger.info("Decoded role from token: {}", role);
//            return ResponseEntity.ok(role);
//        } catch (Exception e) {
//            logger.error("Error while extracting user role from token: {}", e.getMessage(), e);
//            return ResponseEntity.status(403).body("Invalid or expired token");
//        }// returns decoded role like "ROLE_PATIENT" or "ROLE_DOCTOR"
//    }
}
