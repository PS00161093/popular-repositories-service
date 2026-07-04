package com.redcare.github.unit.service;

import com.redcare.github.domain.PopularRepository;
import com.redcare.github.domain.ScoredRepository;
import com.redcare.github.fixtures.RepositoryFixtures;
import com.redcare.github.service.RankingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankingServiceTest {

    private final RankingService rankingService = new RankingService();

    @Test
    void given_emptyScoredList_when_ranked_then_returnsEmptyList() {
        assertThat(rankingService.rank(List.of())).isEmpty();
    }

    @Test
    void given_singleRepository_when_ranked_then_assignsRankOne() {
        List<PopularRepository> ranked = rankingService.rank(List.of(RepositoryFixtures.SCORED_HELLO_ALGO));

        assertThat(ranked).hasSize(1);
        assertThat(ranked.getFirst().rank()).isEqualTo(1);
    }

    @Test
    void given_multipleRepositories_when_ranked_then_sortedByScoreDescending() {
        List<ScoredRepository> scored = List.of(
                RepositoryFixtures.SCORED_CONDUCTOR,
                RepositoryFixtures.SCORED_HELLO_ALGO,
                RepositoryFixtures.SCORED_HELLO_ALGORITHM
        );

        List<PopularRepository> ranked = rankingService.rank(scored);

        assertThat(ranked).extracting(PopularRepository::name)
                .containsExactly("hello-algo", "conductor", "hello-algorithm");
    }

    @Test
    void given_multipleRepositories_when_ranked_then_ranksAreOneBasedAndSequential() {
        List<ScoredRepository> scored = List.of(
                RepositoryFixtures.SCORED_HELLO_ALGO,
                RepositoryFixtures.SCORED_STIRLING_PDF,
                RepositoryFixtures.SCORED_CONDUCTOR
        );

        List<PopularRepository> ranked = rankingService.rank(scored);

        assertThat(ranked).extracting(PopularRepository::rank).containsExactly(1, 2, 3);
    }

    @Test
    void given_repositoriesWithEqualScores_when_ranked_then_bothReceiveDistinctSequentialRanks() {
        List<ScoredRepository> scored = List.of(
                new ScoredRepository(656227652L, "Chat2DB", "OtterMind/Chat2DB",
                        "https://github.com/OtterMind/Chat2DB", 25839, 2836, null, 3.5),
                new ScoredRepository(982587624L, "opendataloader-pdf", "opendataloader-project/opendataloader-pdf",
                        "https://github.com/opendataloader-project/opendataloader-pdf", 26297, 2488, null, 3.5)
        );

        List<PopularRepository> ranked = rankingService.rank(scored);

        assertThat(ranked).extracting(PopularRepository::rank).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void given_scoredRepositories_when_ranked_then_allFieldsAreMappedToRankedResult() {
        PopularRepository ranked = rankingService.rank(List.of(RepositoryFixtures.SCORED_STIRLING_PDF)).getFirst();

        assertThat(ranked.id()).isEqualTo(594155488L);
        assertThat(ranked.name()).isEqualTo("Stirling-PDF");
        assertThat(ranked.fullName()).isEqualTo("Stirling-Tools/Stirling-PDF");
        assertThat(ranked.htmlUrl()).isEqualTo("https://github.com/Stirling-Tools/Stirling-PDF");
        assertThat(ranked.stars()).isEqualTo(86049);
        assertThat(ranked.forks()).isEqualTo(7525);
        assertThat(ranked.pushedAt()).isEqualTo(RepositoryFixtures.SCORED_STIRLING_PDF.pushedAt());
        assertThat(ranked.popularityScore()).isEqualTo(4.12);
        assertThat(ranked.rank()).isEqualTo(1);
    }
}
