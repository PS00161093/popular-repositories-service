package com.redcare.github.client;

import com.redcare.github.config.GithubProperties;
import com.redcare.github.exception.GitHubApiException;
import com.redcare.github.exception.RateLimitExceededException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * HTTP client for the GitHub Search Repositories API.
 * Fetches the top repositories by star count for a given language and creation date filter.
 * Protected by Resilience4j retry and circuit breaker.
 */
@Component
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);

    private final RestClient restClient;
    private final GithubProperties properties;

    public GitHubClient(GithubProperties properties, RestClient gitHubRestClient) {
        this.properties = properties;
        this.restClient = gitHubRestClient;
    }

    /**
     * Searches GitHub for the top repositories matching the given language and creation date.
     * Results are sorted by stars descending. A 403 with {@code X-RateLimit-Remaining: 0}
     * throws {@link RateLimitExceededException}; all other errors throw {@link GitHubApiException}.
     *
     * @param language     programming language (e.g. "java")
     * @param createdAfter only include repositories created after this date (exclusive)
     * @return search result containing repositories and a partial flag
     */
    @CircuitBreaker(name = "github-search")
    @Retry(name = "github-search")
    public GitHubSearchResult searchRepositories(String language, LocalDate createdAfter) {
        String query = "language:" + language + " created:>" + createdAfter;
        log.info("Searching top {} repositories for language={} createdAfter={}", properties.perPage(), language, createdAfter);

        GitHubSearchResponse response = fetchTopRepositories(query);
        boolean partial = response.incompleteResults();

        if (partial) {
            log.warn("GitHub returned incomplete_results=true for query={}", query);
        }

        List<GitHubRepository> repositories = response.items() != null ? response.items() : List.of();
        log.info("GitHub search complete - repositories={} partial={}", repositories.size(), partial);

        return new GitHubSearchResult(repositories, partial);
    }

    private GitHubSearchResponse fetchTopRepositories(String query) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/repositories")
                        .queryParam("q", query)
                        .queryParam("per_page", properties.perPage())
                        .queryParam("sort", "stars")
                        .queryParam("order", "desc")
                        .build())
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.FORBIDDEN.value(), (req, res) -> handleForbidden(res))
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new GitHubApiException("GitHub API error: " + res.getStatusCode());
                })
                .body(GitHubSearchResponse.class);
    }

    // GitHub returns 403 (not 429) for rate limit exhaustion; X-RateLimit-Remaining: 0 distinguishes it from a real permission error
    private void handleForbidden(ClientHttpResponse res) throws IOException {
        String remaining = res.getHeaders().getFirst("X-RateLimit-Remaining");
        if ("0".equals(remaining)) {
            throw new RateLimitExceededException();
        }
        throw new GitHubApiException("GitHub API forbidden: " + res.getStatusCode());
    }
}
