package com.petcarehub.config;

import java.time.Clock;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.annotation.Validated;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OrderExpiryConfig.Settings.class)
public class OrderExpiryConfig {
    @Bean
    Clock businessClock(BookingProperties booking) {
        return Clock.system(booking.getTimezone());
    }

    @Validated
    @ConfigurationProperties("app.order-expiry")
    public record Settings(@DefaultValue("24") @Min(1) int holdHours,
                           @DefaultValue("60000") @Min(1000) long intervalMs) {}
}
