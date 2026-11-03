package com.dis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class ClockConfig {

    private final AtomicReference<Clock> clockReference = new AtomicReference<>(
            Clock.system(ZoneId.of("UTC"))
    );

    @Bean
    @Primary
    public Clock clock() {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return clockReference.get().getZone();
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return clockReference.get().withZone(zone);
            }

            @Override
            public Instant instant() {
                return clockReference.get().instant();
            }
        };
    }

    public void advanceDays(long days) {
        Clock current = clockReference.get();
        Clock advanced = Clock.offset(current, Duration.ofDays(days));
        clockReference.set(advanced);
    }

    public void setClock(Instant instant, ZoneId zoneId) {
        clockReference.set(Clock.fixed(instant, zoneId));
    }

    public void resetClock() {
        clockReference.set(Clock.system(ZoneId.of("UTC")));
    }
}
