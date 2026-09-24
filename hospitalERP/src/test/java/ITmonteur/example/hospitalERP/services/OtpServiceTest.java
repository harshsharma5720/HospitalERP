package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    private SmsService smsService;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        smsService = mock(SmsService.class);
        when(smsService.isEnabled()).thenReturn(true);
        otpService = new OtpService(smsService);
    }

    private String sendAndCaptureOtp(String phone) {
        otpService.sendOtp(phone);
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendOtp(eq(phone), otp.capture(), anyInt());
        return otp.getValue();
    }

    @Test
    void correctOtpVerifiesPhoneOnce() {
        String otp = sendAndCaptureOtp("+919999999999");

        assertThat(otp).matches("\\d{6}");
        assertThat(otpService.verifyOtp("+919999999999", otp)).isTrue();
        assertThat(otpService.isPhoneVerified("+919999999999")).isTrue();
        // An OTP can only be used once
        assertThat(otpService.verifyOtp("+919999999999", otp)).isFalse();

        otpService.consumeVerification("+919999999999");
        assertThat(otpService.isPhoneVerified("+919999999999")).isFalse();
    }

    @Test
    void tooManyWrongAttemptsInvalidateTheOtp() {
        String otp = sendAndCaptureOtp("+919999999998");
        for (int i = 0; i < OtpService.MAX_ATTEMPTS; i++) {
            assertThat(otpService.verifyOtp("+919999999998", "000000".equals(otp) ? "111111" : "000000")).isFalse();
        }
        assertThat(otpService.verifyOtp("+919999999998", otp)).isFalse();
    }

    @Test
    void resendingWithinCooldownIsRefused() {
        sendAndCaptureOtp("+919999999997");
        assertThatThrownBy(() -> otpService.sendOtp("+919999999997")).isInstanceOf(ConflictException.class);
    }
}
