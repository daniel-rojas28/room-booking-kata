package com.example.booking.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** Inject this instead of calling LocalDateTime.now() so tests can fix the time. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
