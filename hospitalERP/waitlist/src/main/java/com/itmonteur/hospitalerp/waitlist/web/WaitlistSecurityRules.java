package com.itmonteur.hospitalerp.waitlist.web;

import com.itmonteur.hospitalerp.identity.ModuleSecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/** The patients' waitlist (docs/WAITLIST_PLAN.md); the desk's is under /api/receptionist, staff's rules. */
@Component
public class WaitlistSecurityRules implements ModuleSecurityRules {

    @Override
    public void areaRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers("/api/waitlist/**").hasRole("PATIENT");
    }
}
