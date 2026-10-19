package com.itmonteur.hospitalerp.inbox.web;

import com.itmonteur.hospitalerp.identity.ModuleSecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/** Everyone logged in has a notification centre; InboxStore keeps each user to their own messages. */
@Component
public class InboxSecurityRules implements ModuleSecurityRules {

    @Override
    public void areaRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers("/api/inbox/**").authenticated();
    }
}
