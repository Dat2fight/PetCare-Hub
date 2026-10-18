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
    // Provider validates without Spring binding errors echoing rejected secret values.
    @lombok.ToString.Exclude
    private String secret;
    @Positive
    private long expirationMs;
}
