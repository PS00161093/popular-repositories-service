package com.redcare.github.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration for GitHub API connectivity, bound from the {@code github.*} prefix.
 * Validated at startup - the application will not start if any property is missing or invalid.
 */
@Validated
@ConfigurationProperties(prefix = "github")
public record GithubProperties(
        @NotBlank String token,
        @NotBlank String baseUrl,
        @Min(1) @Max(100) int perPage
) {
}
