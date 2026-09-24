package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.AuthResponseDTO;
import ITmonteur.example.hospitalERP.dto.LoginRequestDTO;
import ITmonteur.example.hospitalERP.dto.RegisterRequestDTO;
import ITmonteur.example.hospitalERP.services.AuthService;
import ITmonteur.example.hospitalERP.services.OtpService;
import ITmonteur.example.hospitalERP.services.SmsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;
    @Autowired
    private OtpService otpService;
    @Autowired
    private SmsService smsService;

    // Self-registration for patients (staff accounts are created by an admin)
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        logger.info("Register request received for username: {}", request.getUsername());
        return ResponseEntity.ok(this.authService.register(request));
    }

    // User login
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(this.authService.login(request));
    }

    // Lets the register page know whether it must show the OTP step
    @GetMapping("/otp-required")
    public ResponseEntity<Map<String, Object>> otpRequired() {
        return ResponseEntity.ok(Map.of(
                "otpRequired", authService.isOtpRequired(),
                "smsEnabled", smsService.isEnabled()));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> request) {
        if (!smsService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "message", "SMS service is not configured on the server"));
        }
        this.otpService.sendOtp(request.get("phone"));
        return ResponseEntity.ok(Map.of("success", true, "message", "OTP sent successfully"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
        boolean isVerified = this.otpService.verifyOtp(request.get("phone"), request.get("otp"));
        Map<String, Object> response = Map.of(
                "success", isVerified,
                "message", isVerified ? "OTP verified successfully" : "Invalid or expired OTP");
        return isVerified ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
}
