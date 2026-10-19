package com.itmonteur.hospitalerp.identity;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * A login account as Spring Security sees it, with the token version its access tokens must carry
 * (JWTAuthenticationFilter; docs/SAFER_LOGIN_TOKENS_PLAN.md). A deactivated account is disabled.
 */
class AccountDetails extends org.springframework.security.core.userdetails.User {

    private final int tokenVersion;

    AccountDetails(User user) {
        super(user.getUsername(), user.getPassword(), user.isActive(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        this.tokenVersion = user.getTokenVersion();
    }

    int getTokenVersion() {
        return tokenVersion;
    }
}
