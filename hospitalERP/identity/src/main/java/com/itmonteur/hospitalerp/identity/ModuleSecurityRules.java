package com.itmonteur.hospitalerp.identity;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * URL access rules contributed by one module. Each module declares the rules for its own URLs in a
 * bean implementing this interface; SecurityConfig (app) only combines them, so adding a module
 * never means editing a central list.
 *
 * Spring Security applies the first rule that matches, so SecurityConfig adds them in two rounds:
 * first every module's {@link #endpointRules}, then every module's {@link #areaRules}.
 * Endpoint rules of different modules must not overlap, and neither must areas.
 * AccessRulesContractTest (app) freezes the combined result.
 */
public interface ModuleSecurityRules {

    /**
     * Rules for single endpoints or small groups, e.g. the public doctor list. They are applied before
     * every module's area rules, so they may carve an exception out of an area (even another module's).
     */
    default void endpointRules(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
    }

    /** Rules for a whole URL area the module owns, e.g. {@code /api/leaves/**}. */
    default void areaRules(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
    }
}
