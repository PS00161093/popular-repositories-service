package com.redcare.github.service;

import com.redcare.github.client.GitHubRepository;
import com.redcare.github.domain.ScoredRepository;
import com.redcare.github.scoring.ScoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Assigns a popularity score to each repository using the configured {@link ScoringStrategy}.
 */
@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    private final ScoringStrategy scoringStrategy;

    public ScoringService(ScoringStrategy scoringStrategy) {
        this.scoringStrategy = scoringStrategy;
    }

    /**
     * Scores each repository and returns them as {@link ScoredRepository} instances.
     *
     * @param repositories raw repositories from the GitHub API
     * @return scored repositories in the same order as input
     */
    public List<ScoredRepository> score(List<GitHubRepository> repositories) {
        log.debug("Scoring {} repositories", repositories.size());
        return repositories.stream()
                .map(repository -> ScoredRepository.from(repository, scoringStrategy.score(repository)))
                .toList();
    }
}
