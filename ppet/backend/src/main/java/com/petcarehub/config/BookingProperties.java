package com.petcarehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.LocalTime;

@Data
@Component
@ConfigurationProperties(prefix = "app.booking")
public class BookingProperties {
    private LocalTime openingTime = LocalTime.of(9, 0);
    private LocalTime closingTime = LocalTime.of(17, 0);
    private long minimumNoticeMinutes = 0;
}
