package com.redcare.github.domain;

import com.redcare.github.client.GitHubRepository;

import java.time.Instant;

public record ScoredRepository(
        long id,
        String name,
        String fullName,
        String htmlUrl,
        int stars,
        int forks,
        Instant pushedAt,
        double popularityScore) {

    public static ScoredRepository from(GitHubRepository repository, double score) {
        return new ScoredRepository(
                repository.id(), repository.name(), repository.fullName(), repository.htmlUrl(),
                repository.stargazersCount(), repository.forksCount(), repository.pushedAt(),
                score
        );
    }
}
