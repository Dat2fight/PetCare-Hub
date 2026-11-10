package com.petcarehub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "businessAuditTime")
public class AuditConfig {
    @org.springframework.context.annotation.Bean
    org.springframework.data.auditing.DateTimeProvider businessAuditTime(java.time.Clock clock) {
        return () -> java.util.Optional.of(java.time.LocalDateTime.now(clock));
    }
}
