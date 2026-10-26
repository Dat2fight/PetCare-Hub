package com.petcarehub.config;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    // Blank values are safe to report; byte length and exposed-key checks stay in the provider
    // so Spring never echoes a rejected non-empty signing key.
    @lombok.ToString.Exclude
    @jakarta.validation.constraints.NotBlank(message="Cần đặt JWT_SECRET (app.jwt.secret)")
    private String secret;
    @Positive
    private long expirationMs;
}
