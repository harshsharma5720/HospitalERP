package com.itmonteur.hospitalerp.identity.web;

import com.itmonteur.hospitalerp.common.ForbiddenException;
import com.itmonteur.hospitalerp.identity.AuthResponseDTO;
import com.itmonteur.hospitalerp.identity.AuthService;
import com.itmonteur.hospitalerp.identity.ForgotPasswordRequestDTO;
import com.itmonteur.hospitalerp.identity.LoginRequestDTO;
import com.itmonteur.hospitalerp.identity.RegisterRequestDTO;
import com.itmonteur.hospitalerp.identity.ResetPasswordRequestDTO;
import com.itmonteur.hospitalerp.identity.internal.OtpService;
import com.itmonteur.hospitalerp.identity.internal.PasswordResetService;
import com.itmonteur.hospitalerp.notifications.SmsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final OtpService otpService;
    private final SmsService smsService;
    private final PasswordResetService passwordResetService;

    static final String REFRESH_COOKIE = "hospital_refresh";

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthService authService, OtpService otpService, SmsService smsService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.otpService = otpService;
        this.smsService = smsService;
        this.passwordResetService = passwordResetService;
    }

    // Self-registration for patients (staff accounts are created by an admin); logged in at once
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        logger.info("Register request received for username: {}", request.getUsername());
        return withSession(this.authService.register(request));
    }

    // The access token in the body, the refresh token in the httpOnly cookie (docs/SAFER_LOGIN_TOKENS_PLAN.md)
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return withSession(this.authService.login(request));
    }

    // A new access token for the cookie's refresh token, which is replaced; 401 (and the cookie cleared) when it is no good
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                     @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        requireAppRequest(requestedWith);
        return authService.refresh(refreshToken).<ResponseEntity<?>>map(this::withSession)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO))
                        .body(Map.of("success", false, "message", "Your session has ended. Please log in again.")));
    }

    // Logout on this device: the session ends and the cookie is cleared
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                                      @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        requireAppRequest(requestedWith);
        authService.logout(refreshToken);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO))
                .body(Map.of("success", true, "message", "Logged out"));
    }

    private ResponseEntity<AuthResponseDTO> withSession(AuthService.Session session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), session.refreshMaxAge()))
                .body(new AuthResponseDTO(session.accessToken()));
    }

    // httpOnly (no script can read it), SameSite=Strict, sent only to /api/auth; Secure behind HTTPS
    private String refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(cookieSecure).sameSite("Strict")
                .path("/api/auth").maxAge(maxAge).build().toString();
    }

    // The app adds X-Requested-With to refresh and logout; a form on another site can't (with SameSite: no CSRF)
    private static void requireAppRequest(String requestedWith) {
        if (!"XMLHttpRequest".equals(requestedWith)) {
            throw new ForbiddenException("This request must come from the app");
        }
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

    // Step 1 of "forgot password": always answers the same, whether or not the account exists
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.requestReset(request.getIdentifier());
        return ResponseEntity.ok(Map.of("success", true,
                "message", "If an account exists, a reset code has been sent to its registered phone and email."));
    }

    // Step 2: set a new password with the code
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.resetPassword(request.getIdentifier(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("success", true, "message", "Password changed. You can now log in."));
    }
}
