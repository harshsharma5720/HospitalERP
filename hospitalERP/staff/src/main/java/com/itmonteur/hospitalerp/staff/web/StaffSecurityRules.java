package com.itmonteur.hospitalerp.staff.web;

import com.itmonteur.hospitalerp.identity.ModuleSecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * The doctor portal, the front desk and leave requests.
 * The /api/doctor and /api/receptionist areas also hold endpoints of scheduling, appointments and
 * administration (modules above staff); they share these rules.
 */
@Component
public class StaffSecurityRules implements ModuleSecurityRules {

    @Override
    public void endpointRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        // Public doctor directory (also under /api/patient, for the patient pages)
        rules.requestMatchers(HttpMethod.GET,
                "/api/doctor/getAll",
                "/api/doctor/getAllBySpecialization",
                "/api/doctor/getDoctor/**",
                "/api/patient/getAllDoctors",
                "/api/patient/getAllBySpecialization"
        ).permitAll();
    }

    @Override
    public void areaRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers("/api/doctor/**").hasAnyRole("DOCTOR", "ADMIN");
        rules.requestMatchers("/api/receptionist/**").hasAnyRole("RECEPTIONIST", "ADMIN");
        rules.requestMatchers("/api/leaves/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST");
    }
}
