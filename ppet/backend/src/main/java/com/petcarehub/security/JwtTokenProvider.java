package com.petcarehub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import com.petcarehub.config.JwtProperties;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class JwtTokenProvider {

    // Identify formerly committed defaults without retaining their signing material.
    private static final Set<String> EXPOSED_KEY_FINGERPRINTS = Set.of(
            "594c081d23de22b178ba34a9c8551fe23d3a7034d92717a5cb4a054337a4c714",
            "0cd1e952aa5760137d5b4b887912f5947f780cd956ea83e05318fc33fbf98364",
            "c1ccfe09e0bfa58339028291eb588072be45be9a551491949d9caa4614db9e62");
    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.getSecret();
        // Validate at bean creation, not on the first login; never include the value in errors.
        if (secret == null || secret.isBlank() || secret.contains("${")
                || secret.toUpperCase(Locale.ROOT).contains("CHANGE_ME")
                || secret.getBytes(StandardCharsets.UTF_8).length < 32 || isExposed(secret)) {
            throw new IllegalStateException("Set JWT_SECRET (app.jwt.secret) to a new random value of at least 32 UTF-8 bytes; missing, placeholder, short or previously exposed keys are rejected");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isExposed(String secret) {
        try {
            String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8)));
            return EXPOSED_KEY_FINGERPRINTS.contains(fingerprint);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for JWT key validation", ex);
        }
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    public String generateToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + properties.getExpirationMs());

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Date getExpirationFromJWT(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload().getExpiration();
    }
}
