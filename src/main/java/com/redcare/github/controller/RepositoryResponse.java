package com.redcare.github.controller;

import com.redcare.github.domain.PopularRepository;

import java.time.Instant;

public record RepositoryResponse(
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

    public static RepositoryResponse from(PopularRepository repository) {
        return new RepositoryResponse(
                repository.id(), repository.name(), repository.fullName(), repository.htmlUrl(),
                repository.stars(), repository.forks(), repository.pushedAt(),
                repository.popularityScore(), repository.rank()
        );
    }
}
