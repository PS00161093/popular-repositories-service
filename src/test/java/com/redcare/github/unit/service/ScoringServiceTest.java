package com.redcare.github.unit.service;

import com.redcare.github.domain.ScoredRepository;
import com.redcare.github.fixtures.RepositoryFixtures;
import com.redcare.github.scoring.ScoringStrategy;
import com.redcare.github.service.ScoringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock
    private ScoringStrategy scoringStrategy;

    @InjectMocks
    private ScoringService scoringService;

    @Test
    void given_emptyRepositoryList_when_scored_then_returnsEmptyList() {
        assertThat(scoringService.score(List.of())).isEmpty();
    }

    @Test
    void given_repositories_when_scored_then_scoreFromStrategyIsAssignedToEach() {
        given(scoringStrategy.score(RepositoryFixtures.HELLO_ALGO)).willReturn(4.37);
        given(scoringStrategy.score(RepositoryFixtures.STIRLING_PDF)).willReturn(4.12);

        List<ScoredRepository> scored = scoringService.score(List.of(RepositoryFixtures.HELLO_ALGO, RepositoryFixtures.STIRLING_PDF));

        assertThat(scored).extracting(ScoredRepository::popularityScore).containsExactly(4.37, 4.12);
    }

    @Test
    void given_repositories_when_scored_then_inputOrderIsPreserved() {
        given(scoringStrategy.score(RepositoryFixtures.HELLO_ALGO)).willReturn(4.37);
        given(scoringStrategy.score(RepositoryFixtures.STIRLING_PDF)).willReturn(4.12);
        given(scoringStrategy.score(RepositoryFixtures.CONDUCTOR)).willReturn(3.51);

        List<ScoredRepository> scored = scoringService.score(
                List.of(RepositoryFixtures.HELLO_ALGO, RepositoryFixtures.STIRLING_PDF, RepositoryFixtures.CONDUCTOR));

        assertThat(scored).extracting(ScoredRepository::name).containsExactly("hello-algo", "Stirling-PDF", "conductor");
    }

    @Test
    void given_repository_when_scored_then_allFieldsAreMappedToScoredResult() {
        given(scoringStrategy.score(RepositoryFixtures.STIRLING_PDF)).willReturn(4.12);

        ScoredRepository scored = scoringService.score(List.of(RepositoryFixtures.STIRLING_PDF)).getFirst();

        assertThat(scored.id()).isEqualTo(594155488L);
        assertThat(scored.name()).isEqualTo("Stirling-PDF");
        assertThat(scored.fullName()).isEqualTo("Stirling-Tools/Stirling-PDF");
        assertThat(scored.htmlUrl()).isEqualTo("https://github.com/Stirling-Tools/Stirling-PDF");
        assertThat(scored.stars()).isEqualTo(86049);
        assertThat(scored.forks()).isEqualTo(7525);
        assertThat(scored.pushedAt()).isEqualTo(RepositoryFixtures.STIRLING_PDF.pushedAt());
        assertThat(scored.popularityScore()).isEqualTo(4.12);
    }
}
