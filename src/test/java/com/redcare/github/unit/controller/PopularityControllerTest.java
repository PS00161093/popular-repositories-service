package com.redcare.github.unit.controller;

import com.redcare.github.controller.PopularityController;
import com.redcare.github.exception.GitHubApiException;
import com.redcare.github.exception.RateLimitExceededException;
import com.redcare.github.fixtures.RepositoryFixtures;
import com.redcare.github.service.PopularityService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PopularityController.class)
class PopularityControllerTest {

    private static final String ENDPOINT = "/api/v1/repositories/popular";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PopularityService popularityService;

    @Test
    void given_validParameters_when_getPopularRepositories_then_returnsRankedRepositories() throws Exception {
        given(popularityService.getPopularRepositories(any()))
                .willReturn(List.of(RepositoryFixtures.RANKED_HELLO_ALGO, RepositoryFixtures.RANKED_STIRLING_PDF));

        mockMvc.perform(get(ENDPOINT)
                        .param("language", "java")
                        .param("createdAfter", "2020-01-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(561730219))
                .andExpect(jsonPath("$[0].name").value("hello-algo"))
                .andExpect(jsonPath("$[0].full_name").value("krahets/hello-algo"))
                .andExpect(jsonPath("$[0].html_url").value("https://github.com/krahets/hello-algo"))
                .andExpect(jsonPath("$[0].stars").value(128141))
                .andExpect(jsonPath("$[0].forks").value(15260))
                .andExpect(jsonPath("$[0].popularity_score").value(4.37))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[1].id").value(594155488))
                .andExpect(jsonPath("$[1].name").value("Stirling-PDF"))
                .andExpect(jsonPath("$[1].popularity_score").value(4.12))
                .andExpect(jsonPath("$[1].rank").value(2));
    }

    @Test
    void given_noMatchingRepositories_when_getPopularRepositories_then_returnsEmptyList() throws Exception {
        given(popularityService.getPopularRepositories(any())).willReturn(List.of());

        mockMvc.perform(get(ENDPOINT)
                        .param("language", "cobol")
                        .param("createdAfter", "2020-01-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void given_blankLanguage_when_getPopularRepositories_then_rejectsRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("language", "   ")
                        .param("createdAfter", "2020-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void given_missingLanguage_when_getPopularRepositories_then_rejectsRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("createdAfter", "2020-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void given_missingCreatedAfter_when_getPopularRepositories_then_rejectsRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("language", "java"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void given_futureCreatedAfter_when_getPopularRepositories_then_rejectsRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("language", "java")
                        .param("createdAfter", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void given_invalidDateFormat_when_getPopularRepositories_then_rejectsRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("language", "java")
                        .param("createdAfter", "2020-01-0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void given_rateLimitExceeded_when_getPopularRepositories_then_informsClientToRetryLater() throws Exception {
        given(popularityService.getPopularRepositories(any())).willThrow(new RateLimitExceededException());

        mockMvc.perform(get(ENDPOINT)
                        .param("language", "java")
                        .param("createdAfter", "2020-01-01"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("You are rate limited. Please try after some time."));
    }

    @Test
    void given_circuitBreakerOpen_when_getPopularRepositories_then_reportsGitHubUnavailable() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("github-search");
        given(popularityService.getPopularRepositories(any()))
                .willThrow(CallNotPermittedException.createCallNotPermittedException(circuitBreaker));

        mockMvc.perform(get(ENDPOINT)
                        .param("language", "java")
                        .param("createdAfter", "2020-01-01"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Unable to get popular repositories. Please try after some time."));
    }

    @Test
    void given_gitHubApiError_when_getPopularRepositories_then_reportsGitHubUnavailable() throws Exception {
        given(popularityService.getPopularRepositories(any()))
                .willThrow(new GitHubApiException("GitHub API error: 500 INTERNAL_SERVER_ERROR"));

        mockMvc.perform(get(ENDPOINT)
                        .param("language", "java")
                        .param("createdAfter", "2020-01-01"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Unable to get popular repositories. Please try after some time."));
    }
}
