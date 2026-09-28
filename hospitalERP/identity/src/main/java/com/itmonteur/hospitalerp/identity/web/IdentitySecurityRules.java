package com.itmonteur.hospitalerp.identity.web;

import com.itmonteur.hospitalerp.identity.ModuleSecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/** Registration, login, OTP and password reset are public. */
@Component
public class IdentitySecurityRules implements ModuleSecurityRules {

    @Override
    public void areaRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers("/api/auth/**").permitAll();
    }
}
