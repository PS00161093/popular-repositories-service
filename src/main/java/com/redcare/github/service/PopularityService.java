package com.redcare.github.service;

import com.redcare.github.client.GitHubRepository;
import com.redcare.github.domain.PopularRepository;
import com.redcare.github.domain.ScoredRepository;
import com.redcare.github.domain.SearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the popular repositories pipeline: fetch, score, rank.
 * Acts as the single entry point from the controller into the service layer.
 */
@Service
public class PopularityService {

    private static final Logger log = LoggerFactory.getLogger(PopularityService.class);

    private final GitHubSearchService gitHubSearchService;
    private final ScoringService scoringService;
    private final RankingService rankingService;

    public PopularityService(GitHubSearchService gitHubSearchService,
                             ScoringService scoringService,
                             RankingService rankingService) {
        this.gitHubSearchService = gitHubSearchService;
        this.scoringService = scoringService;
        this.rankingService = rankingService;
    }

    /**
     * Returns repositories matching the given criteria, scored and ranked by popularity.
     *
     * @param criteria search parameters (language and creation date lower bound)
     * @return ranked list of popular repositories, sorted by popularity score descending
     */
    public List<PopularRepository> getPopularRepositories(SearchCriteria criteria) {
        log.debug("Starting popularity pipeline for criteria={}", criteria);

        List<GitHubRepository> repositories = gitHubSearchService.search(criteria);
        log.debug("Fetched {} repositories from GitHub", repositories.size());

        List<ScoredRepository> scoredRepositories = scoringService.score(repositories);
        log.debug("Scored {} repositories", scoredRepositories.size());

        List<PopularRepository> rankedRepositories = rankingService.rank(scoredRepositories);
        log.debug("Ranked {} repositories", rankedRepositories.size());

        return rankedRepositories;
    }
}
