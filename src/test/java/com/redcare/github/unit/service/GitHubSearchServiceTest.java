package com.redcare.github.unit.service;

import com.redcare.github.client.GitHubClient;
import com.redcare.github.client.GitHubRepository;
import com.redcare.github.client.GitHubSearchResult;
import com.redcare.github.domain.SearchCriteria;
import com.redcare.github.fixtures.RepositoryFixtures;
import com.redcare.github.service.GitHubSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GitHubSearchServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    private GitHubSearchService gitHubSearchService;
    private ConcurrentMapCache repositoryCache;

    private static final SearchCriteria CRITERIA = new SearchCriteria("java", LocalDate.of(2024, 1, 1));

    @BeforeEach
    void setUp() {
        repositoryCache = new ConcurrentMapCache("repositories");
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(repositoryCache));
        cacheManager.afterPropertiesSet();
        gitHubSearchService = new GitHubSearchService(gitHubClient, cacheManager);
    }

    @Test
    void given_cachedResult_when_searched_then_returnsCachedResultWithoutCallingGitHub() {
        List<GitHubRepository> cachedRepositories = List.of(RepositoryFixtures.HELLO_ALGO);
        repositoryCache.put(CRITERIA, cachedRepositories);

        List<GitHubRepository> result = gitHubSearchService.search(CRITERIA);

        assertThat(result).isEqualTo(cachedRepositories);
        verify(gitHubClient, never()).searchRepositories(CRITERIA.language(), CRITERIA.createdAfter());
    }

    @Test
    void given_cacheMiss_when_searched_then_callsGitHubAndReturnsRepositories() {
        List<GitHubRepository> repositories = List.of(RepositoryFixtures.STIRLING_PDF);
        given(gitHubClient.searchRepositories(CRITERIA.language(), CRITERIA.createdAfter()))
                .willReturn(new GitHubSearchResult(repositories, false));

        List<GitHubRepository> result = gitHubSearchService.search(CRITERIA);

        assertThat(result).isEqualTo(repositories);
    }

    @Test
    void given_fullResponse_when_searched_then_resultIsCachedForSubsequentCalls() {
        List<GitHubRepository> repositories = List.of(RepositoryFixtures.CONDUCTOR);
        given(gitHubClient.searchRepositories(CRITERIA.language(), CRITERIA.createdAfter()))
                .willReturn(new GitHubSearchResult(repositories, false));

        gitHubSearchService.search(CRITERIA);
        gitHubSearchService.search(CRITERIA);

        verify(gitHubClient, times(1)).searchRepositories(CRITERIA.language(), CRITERIA.createdAfter());
    }

    @Test
    void given_partialResponse_when_searched_then_resultIsNotCachedAndClientIsCalledEveryTime() {
        List<GitHubRepository> repositories = List.of(RepositoryFixtures.LANGCHAIN4J);
        given(gitHubClient.searchRepositories(CRITERIA.language(), CRITERIA.createdAfter()))
                .willReturn(new GitHubSearchResult(repositories, true));

        gitHubSearchService.search(CRITERIA);
        gitHubSearchService.search(CRITERIA);

        verify(gitHubClient, times(2)).searchRepositories(CRITERIA.language(), CRITERIA.createdAfter());
    }

    @Test
    void given_partialResponse_when_searched_then_repositoriesAreStillReturned() {
        List<GitHubRepository> repositories = List.of(RepositoryFixtures.SPRING_AI, RepositoryFixtures.SPRING_AI_ALIBABA);
        given(gitHubClient.searchRepositories(CRITERIA.language(), CRITERIA.createdAfter()))
                .willReturn(new GitHubSearchResult(repositories, true));

        List<GitHubRepository> result = gitHubSearchService.search(CRITERIA);

        assertThat(result).isEqualTo(repositories);
    }
}
