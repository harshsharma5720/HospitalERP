package com.itmonteur.hospitalerp.clinical.web;

import com.itmonteur.hospitalerp.identity.ModuleSecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/** Medical records. */
@Component
public class ClinicalSecurityRules implements ModuleSecurityRules {

    @Override
    public void areaRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        // Receptionists are deliberately excluded; ConsultationService also checks who the record belongs to
        rules.requestMatchers("/api/consultations/**").hasAnyRole("PATIENT", "DOCTOR", "ADMIN");
    }
}
