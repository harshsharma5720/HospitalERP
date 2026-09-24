package ITmonteur.example.hospitalERP.configuration;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "twilio")
public class TwilioConfig {

    private String accountSid;
    private String authToken;
    private String trialNumber;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            Twilio.init(accountSid, authToken);
        } else {
            LoggerFactory.getLogger(TwilioConfig.class)
                    .warn("Twilio credentials are not set - SMS and OTP messages will not be sent.");
        }
    }

    public boolean isConfigured() {
        return accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && trialNumber != null && !trialNumber.isBlank();
    }

    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getTrialNumber() {
        return trialNumber;
    }

    public void setTrialNumber(String trialNumber) {
        this.trialNumber = trialNumber;
    }
}
