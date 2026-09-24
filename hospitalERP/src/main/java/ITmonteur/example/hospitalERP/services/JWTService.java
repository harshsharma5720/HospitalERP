package ITmonteur.example.hospitalERP.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService {

    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JWTService(@Value("${jwt.secret:}") String secret,
                      @Value("${jwt.expiration-ms:36000000}") long expirationMs) {
        this.expirationMs = expirationMs;
        if (secret == null || secret.isBlank()) {
            logger.warn("JWT_SECRET is not set - using a random key. All tokens become invalid on every restart. "
                    + "Set JWT_SECRET in .env (e.g. `openssl rand -base64 48`).");
            this.secretKey = Jwts.SIG.HS256.key().build();
        } else {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET must be a Base64 value of at least 32 bytes");
            }
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    // Extract username
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract expiration date
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract any claim
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Generate token
    public String generateToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        // Add role information to the token
        if (userDetails.getAuthorities() != null && !userDetails.getAuthorities().isEmpty()) {
            String role = userDetails.getAuthorities().iterator().next().getAuthority();
            claims.put("role", role);
        }
        String token = createToken(claims, userDetails.getUsername());
        logger.debug("Token generated for user: {}", userDetails.getUsername());
        return token;
    }

    // Validate token (signature and expiry are already verified while parsing)
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    //extract role from token
    public String extractUserRole(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public Long extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        return userId instanceof Number number ? number.longValue() : null;
    }

    // Private helpers

    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    // Throws io.jsonwebtoken.JwtException for expired, malformed or tampered tokens
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
