package ITmonteur.example.hospitalERP.configuration;

import ITmonteur.example.hospitalERP.services.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JWTAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless JWT API, no session cookies to protect
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // ---------- public ----------
                        .requestMatchers("/api/auth/**", "/error", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/doctor/getAll",
                                "/api/doctor/getAllBySpecialization",
                                "/api/doctor/getDoctor/**",
                                "/api/patient/getAllDoctors",
                                "/api/patient/getAllBySpecialization"
                        ).permitAll()

                        // ---------- role based ----------
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/slots/generate/**").hasRole("ADMIN")
                        .requestMatchers("/api/slots/**").authenticated()
                        .requestMatchers("/api/doctor/**").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/api/patient/getAll").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers("/api/patient/**").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers("/api/receptionist/**").hasAnyRole("RECEPTIONIST", "ADMIN")
                        .requestMatchers("/api/leaves/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                        // Medical records: receptionists are deliberately excluded
                        .requestMatchers("/api/consultations/**").hasAnyRole("PATIENT", "DOCTOR", "ADMIN")
                        .requestMatchers(
                                "/appointment/getAll",
                                "/appointment/allPendingAppointments",
                                "/appointment/allCompletedAppointments",
                                "/appointment/appointmentsByDoctor/**"
                        ).hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers("/appointment/getDoctorAppointments").hasRole("DOCTOR")
                        // Remaining appointment endpoints check ownership inside AppointmentService
                        .requestMatchers("/appointment/**").hasAnyRole("PATIENT", "DOCTOR", "ADMIN", "RECEPTIONIST")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to continue"))
                        .accessDeniedHandler((request, response, e) ->
                                writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "You are not allowed to perform this action"))
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\",\"success\":false}");
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // password hashing
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
