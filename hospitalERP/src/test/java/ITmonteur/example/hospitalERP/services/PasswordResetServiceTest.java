package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.Role;
import ITmonteur.example.hospitalERP.entities.User;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private UserRepository userRepository;
    private SmsService smsService;
    private PasswordResetService service;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        smsService = mock(SmsService.class);
        when(smsService.isEnabled()).thenReturn(true);
        user = new User();
        user.setId(3L);
        user.setUsername("asha");
        user.setEmail("asha@example.com");
        user.setPhoneNumber("+919999999999");
        user.setRole(Role.PATIENT);
        user.setPassword(encoder.encode("old-password"));
        when(userRepository.findByUsername("asha")).thenReturn(Optional.of(user));
        // A real OtpService, so the code "sent" by SMS is the one that must be entered
        service = new PasswordResetService(userRepository, new OtpService(smsService), smsService,
                mock(EmailService.class), encoder, new LoginAttemptService());
    }

    private String requestAndCaptureCode() {
        service.requestReset("asha");
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendPasswordResetSms(eq("+919999999999"), code.capture(), anyInt());
        return code.getValue();
    }

    @Test
    void unknownAccountGivesNoErrorAndSendsNothing() {
        assertThatCode(() -> service.requestReset("nobody")).doesNotThrowAnyException();
        verify(smsService, never()).sendPasswordResetSms(any(), any(), anyInt());
    }

    @Test
    void correctCodeChangesThePasswordOnce() {
        String code = requestAndCaptureCode();

        service.resetPassword("asha", code, "new-password");

        assertThat(encoder.matches("new-password", user.getPassword())).isTrue();
        verify(userRepository).save(user);
        // The code cannot be reused
        assertThatThrownBy(() -> service.resetPassword("asha", code, "another-one"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void wrongCodeIsRejected() {
        String code = requestAndCaptureCode();
        String wrong = "000000".equals(code) ? "111111" : "000000";

        assertThatThrownBy(() -> service.resetPassword("asha", wrong, "new-password"))
                .isInstanceOf(BadRequestException.class);
        assertThat(encoder.matches("old-password", user.getPassword())).isTrue();
    }
}
