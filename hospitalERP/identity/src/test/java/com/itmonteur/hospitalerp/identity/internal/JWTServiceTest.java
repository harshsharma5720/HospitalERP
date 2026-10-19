package com.itmonteur.hospitalerp.identity.internal;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Access tokens carry the account's token version (log out everywhere, docs/SAFER_LOGIN_TOKENS_PLAN.md). */
class JWTServiceTest {

    // A new key for every run, as JWT_SECRET would be (Base64, 32 bytes)
    private static final String SECRET = Base64.getEncoder().encodeToString(Jwts.SIG.HS256.key().build().getEncoded());
    private final JWTService jwt = new JWTService(SECRET, 900_000);
    private final UserDetails asha = new User("asha", "x", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));

    @Test
    void aTokenCarriesTheAccountVersionAndItsOtherClaims() {
        String token = jwt.generateToken(asha, 7L, 3);
        assertThat(jwt.extractTokenVersion(token)).isEqualTo(3);
        assertThat(jwt.extractUserId(token)).isEqualTo(7L);
        assertThat(jwt.extractUserRole(token)).isEqualTo("ROLE_PATIENT");
        assertThat(jwt.isTokenValid(token, asha)).isTrue();
    }

    @Test
    void aTokenFromBeforeTheVersionCountsAsVersionZero() {
        long now = System.currentTimeMillis();
        String older = Jwts.builder().subject("asha").claim("userId", 7L).issuedAt(new Date(now))
                .expiration(new Date(now + 60_000)).signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET))).compact();
        assertThat(jwt.extractTokenVersion(older)).isZero();
    }
}
