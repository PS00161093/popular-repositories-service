package com.redcare.github.unit.scoring;

import com.redcare.github.client.GitHubRepository;
import com.redcare.github.config.ScoringProperties;
import com.redcare.github.fixtures.RepositoryFixtures;
import com.redcare.github.scoring.WeightedPopularityScorer;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WeightedPopularityScorerTest {

    private static final ScoringProperties WEIGHTS = new ScoringProperties(0.6, 0.3, 0.1);
    private static final Instant NOW = Instant.parse("2026-07-04T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final WeightedPopularityScorer scorer = new WeightedPopularityScorer(WEIGHTS, FIXED_CLOCK);

    @Test
    void given_repositoryWithStarsForksAndRecentPush_when_scored_then_allThreeComponentsContribute() {
        double score = scorer.score(RepositoryFixtures.CONDUCTOR);

        double daysSince = (double) Duration.between(RepositoryFixtures.CONDUCTOR.pushedAt(), NOW).toDays();
        double expected = 0.6 * Math.log10(31994) + 0.3 * Math.log10(950) + 0.1 * (1.0 / (daysSince + 1));
        assertThat(score).isCloseTo(expected, within(0.0001));
    }

    @Test
    void given_repositoryWithNullPushedAt_when_scored_then_recencyContributionIsZero() {
        double scoreWithPush = scorer.score(RepositoryFixtures.SMART_TUBE);
        double scoreWithoutPush = scorer.score(repositoryWith(30940, 1673, null));

        assertThat(scoreWithoutPush).isLessThan(scoreWithPush);
        double expected = 0.6 * Math.log10(30941) + 0.3 * Math.log10(1674);
        assertThat(scoreWithoutPush).isCloseTo(expected, within(0.0001));
    }

    @Test
    void given_repositoryPushedToday_when_scored_then_recencyScoreIsMaximum() {
        double score = scorer.score(repositoryWith(0, 0, NOW));

        assertThat(score).isCloseTo(0.1, within(0.0001));
    }

    @Test
    void given_repositoryPushedNDaysAgo_when_scored_then_recencyDecaysWithDays() {
        int daysAgo = 9;
        double score = scorer.score(repositoryWith(0, 0, NOW.minusSeconds(daysAgo * 86400L)));

        assertThat(score).isCloseTo(0.1 * (1.0 / (daysAgo + 1)), within(0.0001));
    }

    @Test
    void given_repositoryWithZeroStarsAndForks_when_scored_then_onlyRecencyContributes() {
        double score = scorer.score(repositoryWith(0, 0, NOW));

        assertThat(score).isCloseTo(0.1, within(0.0001));
    }

    @Test
    void given_twoRepositoriesWithDifferentStars_when_scored_then_higherStarRepositoryScoresHigher() {
        assertThat(scorer.score(RepositoryFixtures.HELLO_ALGO)).isGreaterThan(scorer.score(RepositoryFixtures.CONDUCTOR));
    }

    @Test
    void given_twoRepositoriesWithEqualStarsButDifferentForks_when_scored_then_moreForkRepositoryScoresHigher() {
        GitHubRepository moreForks = repositoryWith(23059, 13389, null);
        GitHubRepository fewerForks = repositoryWith(23059, 8432, null);

        assertThat(scorer.score(moreForks)).isGreaterThan(scorer.score(fewerForks));
    }

    @Test
    void given_twoRepositoriesWithEqualStarsAndForksButDifferentPushDates_when_scored_then_recentlyPushedRepositoryScoresHigher() {
        GitHubRepository recentlyPushed = repositoryWith(50000, 3000, NOW.minusSeconds(86400L));
        GitHubRepository pushedYearsAgo = repositoryWith(50000, 3000, NOW.minusSeconds(86400L * 730));

        assertThat(scorer.score(recentlyPushed)).isGreaterThan(scorer.score(pushedYearsAgo));
    }

    private static GitHubRepository repositoryWith(int stars, int forks, Instant pushedAt) {
        return new GitHubRepository(1L, "test-repo", "owner/test-repo",
                "https://github.com/owner/test-repo", stars, forks, pushedAt);
    }
}
