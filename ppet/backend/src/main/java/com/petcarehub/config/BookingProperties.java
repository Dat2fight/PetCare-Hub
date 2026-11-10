package com.petcarehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.LocalTime;

@org.springframework.validation.annotation.Validated
@Data
@Component
@ConfigurationProperties(prefix = "app.booking")
public class BookingProperties {
    @jakarta.validation.constraints.NotNull
    private java.time.ZoneId timezone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
    private LocalTime openingTime = LocalTime.of(9, 0);
    private LocalTime closingTime = LocalTime.of(17, 0);
    @jakarta.validation.constraints.Min(1)
    private int lockTimeoutMs = 5000;
    private long minimumNoticeMinutes = 0;
}
