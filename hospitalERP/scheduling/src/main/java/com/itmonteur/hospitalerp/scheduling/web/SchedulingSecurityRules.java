package com.itmonteur.hospitalerp.scheduling.web;

import com.itmonteur.hospitalerp.identity.ModuleSecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/** Slots. (The weekly schedule endpoints live under /api/doctor and follow staff's rules.) */
@Component
public class SchedulingSecurityRules implements ModuleSecurityRules {

    @Override
    public void endpointRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers("/api/slots/generate/**").hasRole("ADMIN");
    }

    @Override
    public void areaRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers("/api/slots/**").authenticated();
    }
}
