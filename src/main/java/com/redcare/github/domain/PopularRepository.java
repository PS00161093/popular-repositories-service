package com.redcare.github.domain;

import java.time.Instant;

public record PopularRepository(
        long id,
        String name,
        String fullName,
        String htmlUrl,
        int stars,
        int forks,
        Instant pushedAt,
        double popularityScore,
        int rank
) {

    public static PopularRepository from(ScoredRepository scoredRepository, int rank) {
        return new PopularRepository(
                scoredRepository.id(), scoredRepository.name(), scoredRepository.fullName(), scoredRepository.htmlUrl(),
                scoredRepository.stars(), scoredRepository.forks(), scoredRepository.pushedAt(),
                scoredRepository.popularityScore(), rank
        );
    }
}
