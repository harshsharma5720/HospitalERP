package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.AuthResponseDTO;
import ITmonteur.example.hospitalERP.dto.LoginRequestDTO;
import ITmonteur.example.hospitalERP.dto.RegisterRequestDTO;
import ITmonteur.example.hospitalERP.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
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
}
