package com.petcarehub;

import com.petcarehub.config.JwtProperties;
import com.petcarehub.security.JwtTokenProvider;
import com.petcarehub.security.UserDetailsImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JwtSecretConfigurationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    @Import(JwtTokenProvider.class)
    static class ConfigurationUnderTest {}

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withUserConfiguration(ConfigurationUnderTest.class)
                .withInitializer(context -> {
                    // Test scenarios must not inherit an operator's real signing key.
                    context.getEnvironment().getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                    context.getEnvironment().getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                }).withPropertyValues("app.jwt.expiration-ms=60000");
    }

    private String randomSecret() {
        byte[] bytes = new byte[64]; new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private JwtTokenProvider provider(String secret) {
        var properties = new JwtProperties(); properties.setSecret(secret); properties.setExpirationMs(60000);
        return new JwtTokenProvider(properties);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        var principal = new UserDetailsImpl(1L, "jwt-test", "jwt@example.test", "unused", List.of());
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "short", "CHANGE_ME", "CHANGE_ME_CHANGE_ME_CHANGE_ME_CHANGE_ME", "${JWT_SECRET}"})
    void missingBlankShortAndPlaceholderSecretsPreventContextStartup(String secret) {
        var contextRunner = secret == null ? runner() : runner().withPropertyValues("app.jwt.secret=" + secret);
        contextRunner.run(context -> {
            assertNotNull(context.getStartupFailure());
            Throwable cause = context.getStartupFailure();
            while (cause.getCause() != null) cause = cause.getCause();
            assertTrue(cause.getMessage().contains("JWT_SECRET"));
        });
    }

    @Test void rejects31BytesWithoutEchoingTheSecret() {
        String secret = randomSecret().substring(0, 31);
        var failure = assertThrows(IllegalStateException.class, () -> provider(secret));
        assertFalse(failure.getMessage().contains(secret));
        assertTrue(failure.getMessage().contains("JWT_SECRET"));
    }

    @ParameterizedTest @CsvSource({"32,HS256", "48,HS384", "64,HS512"})
    void validKeySignsAndValidatesWithExistingAlgorithmSelection(int bytes, String algorithm) {
        String secret = randomSecret().substring(0, bytes);
        runner().withPropertyValues("app.jwt.secret=" + secret).run(context -> {
            assertNull(context.getStartupFailure());
            var provider = context.getBean(JwtTokenProvider.class);
            String token = provider.generateToken(authentication());
            assertTrue(provider.validateToken(token));
            assertEquals("jwt-test", provider.getUsernameFromJWT(token));
            var signed = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build().parseSignedClaims(token);
            assertEquals(algorithm, signed.getHeader().getAlgorithm());
            assertEquals(60000, signed.getPayload().getExpiration().getTime() - signed.getPayload().getIssuedAt().getTime());
            assertEquals(3, signed.getPayload().size()); // Preserve sub, iat, exp only.
        });
    }

    @Test void rotationRejectsATokenSignedByAnotherRandomKey() {
        var oldProvider = provider(randomSecret()); var newProvider = provider(randomSecret());
        String token = oldProvider.generateToken(authentication());
        assertTrue(oldProvider.validateToken(token));
        assertFalse(newProvider.validateToken(token));
    }

    @Test void expiredTokenIsRejected() {
        String secret = randomSecret(); var provider = provider(secret);
        String token = Jwts.builder().subject("jwt-test").issuedAt(new Date(System.currentTimeMillis() - 120000))
                .expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        assertFalse(provider.validateToken(token));
    }

    @Test void propertiesToStringDoesNotExposeSecret() {
        String secret = randomSecret(); var properties = new JwtProperties(); properties.setSecret(secret);
        assertFalse(properties.toString().contains(secret));
    }

    @ParameterizedTest @ValueSource(strings = {"default", "dev"})
    void runtimeProfilesRequireEnvironmentSecretWithoutFallback(String profile) {
        var profileRunner = runner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.config.location=classpath:/application.yml", "spring.profiles.active=" + profile);
        profileRunner.run(context -> assertNotNull(context.getStartupFailure()));
        profileRunner.withPropertyValues("JWT_SECRET=" + randomSecret()).run(context -> assertNull(context.getStartupFailure()));
    }
}
