package com.redcare.github.client;

import java.time.Instant;

public record GitHubRepository(
        long id,
        String name,
        String fullName,
        String htmlUrl,
        int stargazersCount,
        int forksCount,
        Instant pushedAt
) {
}
