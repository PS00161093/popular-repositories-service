package com.redcare.github.integration;

import com.redcare.github.client.GitHubClient;
import com.redcare.github.client.GitHubSearchResult;
import com.redcare.github.exception.GitHubApiException;
import com.redcare.github.exception.RateLimitExceededException;
import com.redcare.github.fixtures.RepositoryFixtures;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "github.token=test-token"
)
@AutoConfigureMockMvc
class PopularityIntegrationTest {

    private static final String ENDPOINT = "/api/v1/repositories/popular";
    private static final LocalDate CREATED_AFTER = LocalDate.of(2024, 1, 1);
    private static final String LANGUAGE = "java";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private GitHubClient gitHubClient;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("repositories").clear();
    }

    @Test
    void given_repositoriesFromGitHub_when_getPopularRepositories_then_scoredAndRankedByPopularityDescending() throws Exception {
        // Stirling-PDF comes first in the input; hello-algo must end up ranked #1 after scoring
        given(gitHubClient.searchRepositories(LANGUAGE, CREATED_AFTER))
                .willReturn(new GitHubSearchResult(
                        List.of(RepositoryFixtures.STIRLING_PDF, RepositoryFixtures.HELLO_ALGO), false));

        mockMvc.perform(get(ENDPOINT)
                        .param("language", LANGUAGE)
                        .param("createdAfter", CREATED_AFTER.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // hello-algo has more stars, so it must be ranked first regardless of when the test runs
                .andExpect(jsonPath("$[0].name").value("hello-algo"))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].popularity_score").isNumber())
                .andExpect(jsonPath("$[1].name").value("Stirling-PDF"))
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[1].popularity_score").isNumber());
    }

    @Test
    void given_sameRequestMadeTwice_when_getPopularRepositories_then_gitHubCalledOnlyOnce() throws Exception {
        given(gitHubClient.searchRepositories(LANGUAGE, CREATED_AFTER))
                .willReturn(new GitHubSearchResult(List.of(RepositoryFixtures.HELLO_ALGO), false));

        mockMvc.perform(get(ENDPOINT).param("language", LANGUAGE).param("createdAfter", CREATED_AFTER.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get(ENDPOINT).param("language", LANGUAGE).param("createdAfter", CREATED_AFTER.toString()))
                .andExpect(status().isOk());

        verify(gitHubClient, times(1)).searchRepositories(LANGUAGE, CREATED_AFTER);
    }

    @Test
    void given_partialResponseFromGitHub_when_requestedTwice_then_gitHubCalledBothTimes() throws Exception {
        given(gitHubClient.searchRepositories(LANGUAGE, CREATED_AFTER))
                .willReturn(new GitHubSearchResult(List.of(RepositoryFixtures.CONDUCTOR), true));

        mockMvc.perform(get(ENDPOINT).param("language", LANGUAGE).param("createdAfter", CREATED_AFTER.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get(ENDPOINT).param("language", LANGUAGE).param("createdAfter", CREATED_AFTER.toString()))
                .andExpect(status().isOk());

        verify(gitHubClient, times(2)).searchRepositories(LANGUAGE, CREATED_AFTER);
    }

    @Test
    void given_rateLimitExceeded_when_getPopularRepositories_then_informsClientToRetryLater() throws Exception {
        given(gitHubClient.searchRepositories(LANGUAGE, CREATED_AFTER))
                .willThrow(new RateLimitExceededException());

        mockMvc.perform(get(ENDPOINT)
                        .param("language", LANGUAGE)
                        .param("createdAfter", CREATED_AFTER.toString()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("You are rate limited. Please try after some time."));
    }

    @Test
    void given_gitHubApiError_when_getPopularRepositories_then_reportsGitHubUnavailable() throws Exception {
        given(gitHubClient.searchRepositories(LANGUAGE, CREATED_AFTER))
                .willThrow(new GitHubApiException("GitHub API error: 500 INTERNAL_SERVER_ERROR"));

        mockMvc.perform(get(ENDPOINT)
                        .param("language", LANGUAGE)
                        .param("createdAfter", CREATED_AFTER.toString()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Unable to get popular repositories. Please try after some time."));
    }

    @Test
    void given_circuitBreakerOpen_when_getPopularRepositories_then_reportsGitHubUnavailable() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("github-search");
        given(gitHubClient.searchRepositories(LANGUAGE, CREATED_AFTER))
                .willThrow(CallNotPermittedException.createCallNotPermittedException(circuitBreaker));

        mockMvc.perform(get(ENDPOINT)
                        .param("language", LANGUAGE)
                        .param("createdAfter", CREATED_AFTER.toString()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Unable to get popular repositories. Please try after some time."));
    }

    @Test
    void given_blankLanguage_when_getPopularRepositories_then_rejectsRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("language", "   ")
                        .param("createdAfter", CREATED_AFTER.toString()))
                .andExpect(status().isBadRequest());
    }
}
