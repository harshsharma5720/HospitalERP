package com.itmonteur.hospitalerp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Freezes the public HTTP API: every endpoint's method, URL, consumed content type and
 * method-level role check must match src/test/resources/api-endpoints.txt.
 * The frontend depends on these, so refactoring must not change them by accident.
 *
 * Controller class names are deliberately NOT part of the contract, so endpoints can move
 * between controllers/modules as long as the URL stays the same.
 *
 * After an intended API change, regenerate the snapshot:
 *   ./mvnw test -Dtest=EndpointContractTest -Dsurefire.failIfNoSpecifiedTests=false -DupdateEndpointSnapshot=true
 */
@SpringBootTest(properties = {
        "DB_URL=jdbc:h2:mem:endpoints;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "REMINDERS_ENABLED=false"
})
class EndpointContractTest {

    private static final Path SNAPSHOT = Path.of("src/test/resources/api-endpoints.txt");
    private static final String OUR_PACKAGE = HospitalErpApplication.class.getPackageName();

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void endpointsMatchTheSnapshot() throws IOException {
        List<String> actual = currentEndpoints();

        if (Boolean.getBoolean("updateEndpointSnapshot")) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.write(SNAPSHOT, actual, StandardCharsets.UTF_8);
            return;
        }
        assertThat(SNAPSHOT).as("Snapshot missing - run with -DupdateEndpointSnapshot=true once").exists();
        List<String> expected = Files.readAllLines(SNAPSHOT, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank())
                .toList();

        Set<String> removed = new TreeSet<>(expected);
        actual.forEach(removed::remove);
        Set<String> added = new TreeSet<>(actual);
        expected.forEach(added::remove);
        assertThat(removed).as("Endpoints REMOVED or CHANGED compared to api-endpoints.txt").isEmpty();
        assertThat(added).as("Endpoints ADDED compared to api-endpoints.txt (update the snapshot if intended)").isEmpty();
    }

    /** One line per method + URL, e.g. "PUT /api/doctor/update/{id} consumes=multipart/form-data auth=hasRole('ADMIN')". */
    private List<String> currentEndpoints() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = entry.getValue();
            if (!handler.getBeanType().getPackageName().startsWith(OUR_PACKAGE)) {
                continue; // skip framework endpoints such as /error
            }
            RequestMappingInfo info = entry.getKey();
            List<String> methods = info.getMethodsCondition().getMethods().stream()
                    .map(Enum::name).sorted().toList();
            String consumes = info.getConsumesCondition().getConsumableMediaTypes().stream()
                    .map(Object::toString).sorted().collect(Collectors.joining(","));
            PreAuthorize auth = AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), PreAuthorize.class);
            if (auth == null) {
                auth = AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), PreAuthorize.class);
            }
            for (String path : info.getPatternValues()) {
                for (String method : methods.isEmpty() ? List.of("ANY") : methods) {
                    StringBuilder line = new StringBuilder(method).append(' ').append(path);
                    if (!consumes.isEmpty()) {
                        line.append(" consumes=").append(consumes);
                    }
                    if (auth != null) {
                        line.append(" auth=").append(auth.value());
                    }
                    lines.add(line.toString());
                }
            }
        }
        Collections.sort(lines);
        return lines;
    }
}
