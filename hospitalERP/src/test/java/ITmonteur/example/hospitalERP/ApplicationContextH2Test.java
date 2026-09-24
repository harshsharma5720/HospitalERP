package ITmonteur.example.hospitalERP;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** Starts the whole application on an in-memory H2 database (no MySQL/Twilio/SMTP needed). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:context;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "OTP_REQUIRED=false",
        "ADMIN_USERNAME=rootadmin", "ADMIN_PASSWORD=change-me-123"
})
class ApplicationContextH2Test {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void publicRegistrationAlwaysCreatesAPatient() {
        String body = """
                {"username":"mallory","email":"mallory@example.com","password":"secret123",
                 "phoneNumber":"+919876543210","role":"ADMIN"}""";
        ResponseEntity<String> register = rest.postForEntity("/api/auth/register", json(body), String.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> login = rest.postForEntity("/api/auth/login",
                json("{\"username\":\"mallory\",\"password\":\"secret123\"}"), String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = login.getBody().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        // The "ADMIN" role in the request was ignored, so admin endpoints are forbidden
        var headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> admin = rest.exchange("/api/admin/allUsers", org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers), String.class);
        assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> mine = rest.exchange("/appointment/getPatientAppointments",
                org.springframework.http.HttpMethod.GET, new org.springframework.http.HttpEntity<>(headers), String.class);
        assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void bootstrapAdminCanUseAdminEndpoints() {
        ResponseEntity<String> login = rest.postForEntity("/api/auth/login",
                json("{\"username\":\"rootadmin\",\"password\":\"change-me-123\"}"), String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = login.getBody().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        var headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> users = rest.exchange("/api/admin/allUsers", org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers), String.class);
        assertThat(users.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(users.getBody()).contains("rootadmin").doesNotContain("password");
    }

    @Test
    void wrongPasswordReturns401() {
        ResponseEntity<String> login = rest.postForEntity("/api/auth/login",
                json("{\"username\":\"nobody\",\"password\":\"wrong\"}"), String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static org.springframework.http.HttpEntity<String> json(String body) {
        var headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new org.springframework.http.HttpEntity<>(body, headers);
    }
}
