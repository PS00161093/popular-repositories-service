package com.redcare.github.service;

import com.redcare.github.client.GitHubClient;
import com.redcare.github.client.GitHubRepository;
import com.redcare.github.client.GitHubSearchResult;
import com.redcare.github.domain.SearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides GitHub repository search with Caffeine-backed caching.
 * Returns cached results on a hit; fetches from the GitHub API on a miss.
 * Partial responses (incomplete_results=true) are never cached.
 */
@Service
public class GitHubSearchService {

    private static final Logger log = LoggerFactory.getLogger(GitHubSearchService.class);
    private static final String CACHE_NAME = "repositories";

    private final GitHubClient gitHubClient;
    private final Cache repositoryCache;

    public GitHubSearchService(GitHubClient gitHubClient, CacheManager cacheManager) {
        this.gitHubClient = gitHubClient;
        this.repositoryCache = cacheManager.getCache(CACHE_NAME);
    }

    /**
     * Returns the top repositories matching the given criteria.
     * Serves from cache if available, otherwise delegates to the GitHub API.
     *
     * @param criteria search parameters (language and creation date lower bound)
     * @return list of matching repositories, never null
     */
    public List<GitHubRepository> search(SearchCriteria criteria) {
        return Optional.ofNullable(repositoryCache.get(criteria))
                .map(wrapper -> ((CachedRepositories) wrapper.get()).repositories())
                .orElseGet(() -> fetchAndCache(criteria));
    }

    private List<GitHubRepository> fetchAndCache(SearchCriteria criteria) {
        log.debug("Cache miss for criteria={}, fetching from GitHub", criteria);
        GitHubSearchResult result = gitHubClient.searchRepositories(criteria.language(), criteria.createdAfter());
        cacheIfFullResponse(criteria, result);

        return result.repositories();
    }

    private void cacheIfFullResponse(SearchCriteria criteria, GitHubSearchResult result) {
        if (result.partial()) {
            log.warn("GitHub returned incomplete results for criteria={} - skipping cache, {} repositories returned", criteria, result.repositories().size());
        } else {
            log.debug("Caching {} repositories for criteria={}", result.repositories().size(), criteria);
            repositoryCache.put(criteria, new CachedRepositories(result.repositories()));
        }
    }

    private record CachedRepositories(List<GitHubRepository> repositories) {}
}
