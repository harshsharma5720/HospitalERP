package com.itmonteur.hospitalerp.administration.web;

import com.itmonteur.hospitalerp.identity.ModuleSecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * The admin area. (Account deletion lives under /api/patient, /api/doctor and /api/receptionist, follows
 * those areas' rules and adds @PreAuthorize where only admins may delete.)
 */
@Component
public class AdministrationSecurityRules implements ModuleSecurityRules {

    @Override
    public void areaRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers("/api/admin/**").hasRole("ADMIN");
    }
}
