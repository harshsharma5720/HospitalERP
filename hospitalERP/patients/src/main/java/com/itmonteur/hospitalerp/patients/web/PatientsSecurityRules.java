package com.itmonteur.hospitalerp.patients.web;

import com.itmonteur.hospitalerp.identity.ModuleSecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Patient profiles and relatives. The /api/patient area also holds the public doctor directory
 * (staff, made public by its endpoint rules) and account deletion (administration).
 */
@Component
public class PatientsSecurityRules implements ModuleSecurityRules {

    @Override
    public void endpointRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers("/api/patient/getAll").hasAnyRole("ADMIN", "RECEPTIONIST");
    }

    @Override
    public void areaRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers("/api/patient/**").hasAnyRole("PATIENT", "ADMIN");
    }
}
