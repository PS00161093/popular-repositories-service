package com.redcare.github.scoring;

import com.redcare.github.client.GitHubRepository;
import com.redcare.github.config.ScoringProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scores a repository using a weighted combination of stars, forks, and recency.
 *
 * <pre>
 * score = (stars_weight  × log10(stars + 1))
 *       + (forks_weight  × log10(forks + 1))
 *       + (recency_weight × 1.0 / (daysSinceLastPush + 1))
 * </pre>
 *
 * Weights are configurable via {@link ScoringProperties}. Log10 dampens the
 * outsized influence of repositories with very high star/fork counts.
 * Repositories with a null {@code pushed_at} receive a recency score of 0.0.
 */
@Component
public class WeightedPopularityScorer implements ScoringStrategy {

    private static final Logger log = LoggerFactory.getLogger(WeightedPopularityScorer.class);

    private final ScoringProperties properties;
    private final Clock clock;

    public WeightedPopularityScorer(ScoringProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public double score(GitHubRepository repository) {
        double starScore = Math.log10(repository.stargazersCount() + 1);
        double forkScore = Math.log10(repository.forksCount() + 1);
        double recencyScore = computeRecencyScore(repository);

        double totalScore = properties.stars() * starScore
                + properties.forks() * forkScore
                + properties.recency() * recencyScore;

        log.debug("Scored repository={} stars={} forks={} recencyScore={} totalScore={}",
                repository.fullName(), repository.stargazersCount(), repository.forksCount(), recencyScore, totalScore);

        return totalScore;
    }

    /**
     * Computes a recency score based on days since the last push.
     * The GitHub API defines {@code pushed_at} as nullable (string or null) - repositories
     * that have never had a push event will have a null value. These receive a score of 0.0.
     */
    private double computeRecencyScore(GitHubRepository repository) {
            if (repository.pushedAt() == null) {
            log.warn("Repository={} has null pushedAt - recency score defaulting to 0.0", repository.fullName());
            return 0.0;
        }

        long daysSincePush = ChronoUnit.DAYS.between(repository.pushedAt(), Instant.now(clock));

        return 1.0 / (daysSincePush + 1);
    }
}
