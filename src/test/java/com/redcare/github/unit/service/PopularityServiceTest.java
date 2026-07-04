package com.redcare.github.unit.service;

import com.redcare.github.client.GitHubRepository;
import com.redcare.github.domain.PopularRepository;
import com.redcare.github.domain.ScoredRepository;
import com.redcare.github.domain.SearchCriteria;
import com.redcare.github.fixtures.RepositoryFixtures;
import com.redcare.github.service.GitHubSearchService;
import com.redcare.github.service.PopularityService;
import com.redcare.github.service.RankingService;
import com.redcare.github.service.ScoringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PopularityServiceTest {

    @Mock
    private GitHubSearchService gitHubSearchService;

    @Mock
    private ScoringService scoringService;

    @Mock
    private RankingService rankingService;

    @InjectMocks
    private PopularityService popularityService;

    private static final SearchCriteria CRITERIA = new SearchCriteria("java", LocalDate.of(2024, 1, 1));

    private static final List<GitHubRepository> FETCHED = List.of(RepositoryFixtures.HELLO_ALGO, RepositoryFixtures.STIRLING_PDF);
    private static final List<ScoredRepository> SCORED = List.of(RepositoryFixtures.SCORED_HELLO_ALGO, RepositoryFixtures.SCORED_STIRLING_PDF);
    private static final List<PopularRepository> RANKED = List.of(RepositoryFixtures.RANKED_HELLO_ALGO, RepositoryFixtures.RANKED_STIRLING_PDF);

    @Test
    void given_noRepositoriesFound_when_popularRepositoriesRequested_then_returnsEmptyList() {
        given(gitHubSearchService.search(CRITERIA)).willReturn(List.of());
        given(scoringService.score(List.of())).willReturn(List.of());
        given(rankingService.rank(List.of())).willReturn(List.of());

        assertThat(popularityService.getPopularRepositories(CRITERIA)).isEmpty();
    }

    @Test
    void given_repositoriesFromGitHub_when_popularRepositoriesRequested_then_repositoriesArePassedToScoring() {
        given(gitHubSearchService.search(CRITERIA)).willReturn(FETCHED);
        given(scoringService.score(FETCHED)).willReturn(List.of());
        given(rankingService.rank(List.of())).willReturn(List.of());

        popularityService.getPopularRepositories(CRITERIA);

        verify(scoringService).score(FETCHED);
    }

    @Test
    void given_scoredRepositories_when_popularRepositoriesRequested_then_scoredRepositoriesArePassedToRanking() {
        given(gitHubSearchService.search(CRITERIA)).willReturn(FETCHED);
        given(scoringService.score(FETCHED)).willReturn(SCORED);
        given(rankingService.rank(SCORED)).willReturn(List.of());

        popularityService.getPopularRepositories(CRITERIA);

        verify(rankingService).rank(SCORED);
    }

    @Test
    void given_rankedRepositories_when_popularRepositoriesRequested_then_rankedResultIsReturned() {
        given(gitHubSearchService.search(CRITERIA)).willReturn(FETCHED);
        given(scoringService.score(FETCHED)).willReturn(SCORED);
        given(rankingService.rank(SCORED)).willReturn(RANKED);

        assertThat(popularityService.getPopularRepositories(CRITERIA)).isEqualTo(RANKED);
    }

    @Test
    void given_validCriteria_when_popularRepositoriesRequested_then_allStagesAreExecutedOnce() {
        given(gitHubSearchService.search(CRITERIA)).willReturn(FETCHED);
        given(scoringService.score(FETCHED)).willReturn(SCORED);
        given(rankingService.rank(SCORED)).willReturn(List.of());

        popularityService.getPopularRepositories(CRITERIA);

        verify(gitHubSearchService).search(CRITERIA);
        verify(scoringService).score(FETCHED);
        verify(rankingService).rank(SCORED);
    }
}
