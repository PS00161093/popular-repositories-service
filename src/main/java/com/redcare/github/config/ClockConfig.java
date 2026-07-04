package com.redcare.github.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides a UTC {@link Clock} bean for injection into time-sensitive components.
 * Using an injected clock instead of {@code Instant.now()} makes time-dependent logic testable.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
