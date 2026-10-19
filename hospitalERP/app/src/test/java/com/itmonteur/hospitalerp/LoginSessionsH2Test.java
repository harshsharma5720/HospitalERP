package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmonteur.hospitalerp.identity.internal.RefreshTokenCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Login sessions (docs/SAFER_LOGIN_TOKENS_PLAN.md) over HTTP: the refresh cookie, rotation, reuse, logout, a
 * deactivated account, log out everywhere, the nightly clean-up. Same configuration as FeatureFlowH2Test, so both share one application
 * context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class LoginSessionsH2Test {

    private static final String COOKIE = "hospital_refresh";

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RefreshTokenCleanup cleanup;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    private ResponseEntity<String> post(String url, String body, String refreshCookie, boolean fromTheApp) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (refreshCookie != null) {
            headers.add(HttpHeaders.COOKIE, COOKIE + "=" + refreshCookie);
        }
        if (fromTheApp) {
            headers.add("X-Requested-With", "XMLHttpRequest");
        }
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> login(String username, String password) {
        ResponseEntity<String> res = post("/api/auth/login", "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", null, false);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return res;
    }

    private ResponseEntity<String> refresh(String refreshCookie) {
        return post("/api/auth/refresh", null, refreshCookie, true);
    }

    private static String setCookie(ResponseEntity<String> res) {
        List<String> cookies = res.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).as("Set-Cookie").isNotNull();
        return cookies.stream().filter(c -> c.startsWith(COOKIE + "=")).findFirst().orElseThrow();
    }

    private static String cookieValue(ResponseEntity<String> res) {
        String cookie = setCookie(res);
        return cookie.substring(COOKIE.length() + 1, cookie.indexOf(';'));
    }

    private JsonNode claims(ResponseEntity<String> res) throws Exception {
        String token = json.readTree(res.getBody()).get("token").asText();
        return json.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
    }

    private String accessToken(ResponseEntity<String> res) throws Exception {
        return json.readTree(res.getBody()).get("token").asText();
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void aLoginGivesAShortAccessTokenAndAnHttpOnlyRefreshCookie() throws Exception {
        api.staff("desksession", "RECEPTIONIST");
        ResponseEntity<String> staff = login("desksession", "staff-123");
        String cookie = setCookie(staff);
        assertThat(cookie).contains("HttpOnly", "SameSite=Strict", "Path=/api/auth", "Max-Age=" + 12 * 60 * 60); // staff: 12 hours
        assertThat(cookie).doesNotContain("Secure"); // AUTH_COOKIE_SECURE is off for plain http
        JsonNode claims = claims(staff);
        assertThat(claims.get("exp").asLong() - claims.get("iat").asLong()).as("15 minutes").isEqualTo(900);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE token_hash = ?", Integer.class,
                sha256(cookieValue(staff)))).as("only the hash is stored").isEqualTo(1);

        ResponseEntity<String> registered = post("/api/auth/register", "{\"username\":\"sessionpatient\",\"email\":"
                + "\"sessionpatient@example.com\",\"password\":\"secret-123\",\"phoneNumber\":\"+919876543210\"}", null, false);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(setCookie(registered)).contains("HttpOnly", "Max-Age=" + 7 * 24 * 60 * 60); // a patient: 7 days
    }

    @Test
    void refreshingReplacesTheCookieAndAnOldOneUsedLaterEndsTheLogin() throws Exception {
        api.registerPatient("sessionrotator");
        String first = cookieValue(login("sessionrotator", "secret-123"));

        ResponseEntity<String> refreshed = refresh(first);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String second = cookieValue(refreshed);
        assertThat(second).isNotEqualTo(first);
        assertThat(api.call(HttpMethod.GET, "/appointment/getPatientAppointments", accessToken(refreshed), null)
                .getStatusCode()).as("the new access token works").isEqualTo(HttpStatus.OK);

        assertThat(post("/api/auth/refresh", null, second, false).getStatusCode()).as("not from the app")
                .isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> otherTab = refresh(first);
        assertThat(otherTab.getStatusCode()).as("two tabs at once").isEqualTo(HttpStatus.OK);
        String third = cookieValue(otherTab);

        // The first token replaced a minute ago and used again: copied, so the login ends
        jdbc.update("UPDATE refresh_tokens SET replaced_at = ? WHERE token_hash = ?", LocalDateTime.now().minusMinutes(1), sha256(first));
        ResponseEntity<String> stolen = refresh(first);
        assertThat(stolen.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(setCookie(stolen)).contains("Max-Age=0");
        assertThat(json.readTree(stolen.getBody()).get("message").asText()).isEqualTo("Your session has ended. Please log in again.");
        assertThat(refresh(second).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(third).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(null).getStatusCode()).as("no cookie").isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh("made-up").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutEndsTheLoginAndADeactivatedAccountCantRefresh() throws Exception {
        api.registerPatient("sessionleaver");
        String cookie = cookieValue(login("sessionleaver", "secret-123"));
        assertThat(post("/api/auth/logout", null, cookie, false).getStatusCode()).as("not from the app").isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> out = post("/api/auth/logout", null, cookie, true);
        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(setCookie(out)).contains("Max-Age=0");
        assertThat(refresh(cookie).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post("/api/auth/logout", null, null, true).getStatusCode()).as("already out").isEqualTo(HttpStatus.OK);

        ResponseEntity<String> again = login("sessionleaver", "secret-123");
        long userId = claims(again).get("userId").asLong();
        assertThat(api.call(HttpMethod.PUT, "/api/admin/users/" + userId + "/deactivate", api.admin(), null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(refresh(cookieValue(again)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpStatus myAppointments(String accessToken) {
        return HttpStatus.valueOf(api.call(HttpMethod.GET, "/appointment/getPatientAppointments", accessToken, null)
                .getStatusCode().value());
    }

    @Test
    void logOutOfAllDevicesEndsEveryLoginAndRefusesTheAccessTokensAtOnce() throws Exception {
        api.registerPatient("sessioneverywhere");
        ResponseEntity<String> phone = login("sessioneverywhere", "secret-123");
        ResponseEntity<String> laptop = login("sessioneverywhere", "secret-123");
        assertThat(myAppointments(accessToken(phone))).isEqualTo(HttpStatus.OK);
        assertThat(myAppointments(accessToken(laptop))).isEqualTo(HttpStatus.OK);
        assertThat(post("/api/auth/logout-all", null, null, true).getStatusCode()).as("needs a login")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> out = api.call(HttpMethod.POST, "/api/auth/logout-all", accessToken(phone), null);
        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(setCookie(out)).contains("Max-Age=0");
        assertThat(myAppointments(accessToken(phone))).as("not after 15 minutes: at once").isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(myAppointments(accessToken(laptop))).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(cookieValue(phone)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(cookieValue(laptop)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(jdbc.queryForList("SELECT DISTINCT end_reason FROM refresh_tokens WHERE user_id = ?", String.class,
                claims(phone).get("userId").asLong())).containsExactly("LOGOUT_ALL");

        ResponseEntity<String> back = login("sessioneverywhere", "secret-123");
        assertThat(myAppointments(accessToken(back))).as("a new login works").isEqualTo(HttpStatus.OK);
        ResponseEntity<String> refreshed = refresh(cookieValue(back));
        assertThat(myAppointments(accessToken(refreshed))).as("its refreshed token too").isEqualTo(HttpStatus.OK);
    }

    @Test
    void aReactivatedAccountDoesntGetItsOldLoginsBack() throws Exception {
        api.registerPatient("sessionreturner");
        ResponseEntity<String> before = login("sessionreturner", "secret-123");
        long userId = claims(before).get("userId").asLong();
        String admin = api.admin();
        assertThat(api.call(HttpMethod.PUT, "/api/admin/users/" + userId + "/deactivate", admin, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(api.call(HttpMethod.PUT, "/api/admin/users/" + userId + "/reactivate", admin, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(myAppointments(accessToken(before))).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(cookieValue(before)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(myAppointments(accessToken(login("sessionreturner", "secret-123")))).isEqualTo(HttpStatus.OK);
    }

    private boolean stored(String refreshToken) throws Exception {
        return jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE token_hash = ?", Integer.class,
                sha256(refreshToken)) == 1;
    }

    @Test
    void theNightlyCleanUpDeletesTokensExpiredOrEndedOverThirtyDaysAgo() throws Exception {
        api.registerPatient("sessionsweeper");
        String expired = cookieValue(login("sessionsweeper", "secret-123"));
        String ended = cookieValue(login("sessionsweeper", "secret-123"));
        String endedLately = cookieValue(login("sessionsweeper", "secret-123"));
        String live = cookieValue(login("sessionsweeper", "secret-123"));
        LocalDateTime overAMonthAgo = LocalDateTime.now().minusDays(31);
        jdbc.update("UPDATE refresh_tokens SET created_at = ?, expires_at = ? WHERE token_hash = ?",
                overAMonthAgo.minusDays(7), overAMonthAgo, sha256(expired));
        jdbc.update("UPDATE refresh_tokens SET ended_at = ?, end_reason = 'LOGOUT' WHERE token_hash = ?", overAMonthAgo, sha256(ended));
        jdbc.update("UPDATE refresh_tokens SET ended_at = ?, end_reason = 'LOGOUT' WHERE token_hash = ?",
                LocalDateTime.now().minusDays(29), sha256(endedLately));

        cleanup.deleteOldTokens();

        assertThat(stored(expired)).isFalse();
        assertThat(stored(ended)).isFalse();
        assertThat(stored(endedLately)).as("kept for 30 days").isTrue();
        assertThat(stored(live)).isTrue();
        assertThat(refresh(live).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
