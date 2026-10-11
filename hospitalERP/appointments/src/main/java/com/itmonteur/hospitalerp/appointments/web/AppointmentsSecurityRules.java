package com.itmonteur.hospitalerp.appointments.web;

import com.itmonteur.hospitalerp.identity.ModuleSecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Booking under /appointment, and the public waiting-room screen. (Doctor and front-desk appointment endpoints live
 * under /api/doctor and /api/receptionist and follow staff's rules.)
 */
@Component
public class AppointmentsSecurityRules implements ModuleSecurityRules {

    @Override
    public void endpointRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers(
                "/appointment/getAll",
                "/appointment/allPendingAppointments",
                "/appointment/allCompletedAppointments",
                "/appointment/appointmentsByDoctor/**"
        ).hasAnyRole("ADMIN", "RECEPTIONIST");
        rules.requestMatchers("/appointment/getDoctorAppointments").hasRole("DOCTOR");
        // The waiting-room screen: tokens only, shown without a login (docs/TODAYS_QUEUE_PLAN.md)
        rules.requestMatchers(HttpMethod.GET, "/api/queue/screen").permitAll();
    }

    @Override
    public void areaRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        // Remaining appointment endpoints check ownership inside AppointmentService
        rules.requestMatchers("/appointment/**").hasAnyRole("PATIENT", "DOCTOR", "ADMIN", "RECEPTIONIST");
    }
}
