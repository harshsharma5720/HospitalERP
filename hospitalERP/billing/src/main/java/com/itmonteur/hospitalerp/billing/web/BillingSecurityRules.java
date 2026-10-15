package com.itmonteur.hospitalerp.billing.web;

import com.itmonteur.hospitalerp.identity.ModuleSecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * The patient's own bills, and Razorpay's webhook. The front desk's bills are under /api/receptionist (staff's
 * rules), the fees under /api/admin (administration's).
 */
@Component
public class BillingSecurityRules implements ModuleSecurityRules {

    @Override
    public void endpointRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        // Razorpay calls it without a login; OnlinePaymentService checks its signature
        rules.requestMatchers(HttpMethod.POST, "/api/payments/razorpay/webhook").permitAll();
    }

    @Override
    public void areaRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        // MyBillService also keeps each patient to their own bills
        rules.requestMatchers("/api/bills/my/**").hasRole("PATIENT");
    }
}
