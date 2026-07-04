package com.redcare.github.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration for the popularity scoring weights, bound from the {@code scoring.weights.*} prefix.
 * All weights must be positive and must sum to exactly 1.0. Validated at startup.
 */
@Validated
@ConfigurationProperties(prefix = "scoring.weights")
public record ScoringProperties(
        @Positive double stars,
        @Positive double forks,
        @Positive double recency
) {
    public ScoringProperties {
        long roundedSum = Math.round((stars + forks + recency) * 100);
        if (roundedSum != 100) {
            throw new IllegalArgumentException(
                    "Scoring weights must sum to 1.0 but got " + (roundedSum / 100.0) + " (stars=" + stars + ", forks=" + forks + ", recency=" + recency + ")");
        }
    }
}
