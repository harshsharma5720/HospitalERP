package com.itmonteur.hospitalerp;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Freezes the URL access rules of SecurityConfig: for every endpoint in api-endpoints.txt (plus a few
 * non-controller paths), whether an anonymous caller and each role get past the URL rules.
 * Only the URL rules are checked (not @PreAuthorize or ownership checks inside services), and no
 * controller is called. Snapshot: src/test/resources/api-access.txt ("+" = allowed, "-" = denied).
 *
 * After an intended change of the access rules, regenerate it:
 *   ./mvnw test -Dtest=AccessRulesContractTest -Dsurefire.failIfNoSpecifiedTests=false -DupdateAccessSnapshot=true
 */
@SpringBootTest(properties = {
        "DB_URL=jdbc:h2:mem:access;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "REMINDERS_ENABLED=false"
})
class AccessRulesContractTest {

    private static final Path ENDPOINTS = Path.of("src/test/resources/api-endpoints.txt");
    private static final Path SNAPSHOT = Path.of("src/test/resources/api-access.txt");
    // Paths that are not controller endpoints but have their own rules
    private static final List<String> EXTRA = List.of(
            "GET /uploads/profileImages/photo.png", "GET /actuator/health", "GET /error", "GET /api/unknown");
    private static final String[] ROLES = {"PATIENT", "DOCTOR", "RECEPTIONIST", "ADMIN"};

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void accessRulesMatchTheSnapshot() throws IOException {
        List<String> actual = currentAccessRules();

        if (Boolean.getBoolean("updateAccessSnapshot")) {
            Files.write(SNAPSHOT, actual, StandardCharsets.UTF_8);
            return;
        }
        assertThat(SNAPSHOT).as("Snapshot missing - run with -DupdateAccessSnapshot=true once").exists();
        List<String> expected = Files.readAllLines(SNAPSHOT, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank())
                .toList();

        Set<String> removed = new TreeSet<>(expected);
        actual.forEach(removed::remove);
        Set<String> added = new TreeSet<>(actual);
        expected.forEach(added::remove);
        assertThat(removed).as("Access rules CHANGED compared to api-access.txt (expected lines)").isEmpty();
        assertThat(added).as("Access rules CHANGED compared to api-access.txt (actual lines)").isEmpty();
    }

    private List<String> currentAccessRules() throws IOException {
        AuthorizationManager<HttpServletRequest> rules = securityFilterChain.getFilters().stream()
                .filter(AuthorizationFilter.class::isInstance)
                .map(filter -> ((AuthorizationFilter) filter).getAuthorizationManager())
                .findFirst()
                .orElseThrow();

        Map<String, Authentication> callers = new LinkedHashMap<>();
        callers.put("anonymous", new AnonymousAuthenticationToken("key", "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        for (String role : ROLES) {
            callers.put(role.toLowerCase(), UsernamePasswordAuthenticationToken.authenticated(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        }

        List<String> endpoints = Stream.concat(
                        Files.readAllLines(ENDPOINTS, StandardCharsets.UTF_8).stream()
                                .filter(line -> !line.isBlank())
                                .map(line -> line.split(" ")[0] + " " + line.split(" ")[1]),
                        EXTRA.stream())
                .distinct()
                .sorted()
                .toList();

        List<String> lines = new ArrayList<>();
        for (String endpoint : endpoints) {
            String method = endpoint.split(" ")[0];
            String path = endpoint.split(" ")[1].replaceAll("\\{[^}]+}", "1");
            StringBuilder line = new StringBuilder(endpoint);
            for (Map.Entry<String, Authentication> caller : callers.entrySet()) {
                MockHttpServletRequest request = new MockHttpServletRequest(method, path);
                request.setServletPath(path);
                AuthorizationResult result = rules.authorize(caller::getValue, request);
                boolean allowed = result != null && result.isGranted();
                line.append(' ').append(caller.getKey()).append(allowed ? "+" : "-");
            }
            lines.add(line.toString());
        }
        return lines;
    }
}
