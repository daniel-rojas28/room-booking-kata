package com.example.booking;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Every test runs at Monday 2 March 2026, 09:00. */
@TestConfiguration
public class FixedClockConfig {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 2, 9, 0);

    @Bean
    @Primary
    public Clock fixedClock() {
        ZoneId zone = ZoneId.systemDefault();
        return Clock.fixed(NOW.atZone(zone).toInstant(), zone);
    }
}
