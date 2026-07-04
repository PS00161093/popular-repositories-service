package com.redcare.github.controller;

import com.redcare.github.domain.SearchCriteria;
import com.redcare.github.service.PopularityService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller exposing the popular repositories endpoint.
 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class PopularityController {

    private static final Logger log = LoggerFactory.getLogger(PopularityController.class);

    private final PopularityService popularityService;

    public PopularityController(PopularityService popularityService) {
        this.popularityService = popularityService;
    }

    /**
     * Returns the top repositories for the given language, created after the specified date,
     * ranked by popularity score descending.
     *
     * @param language     programming language to filter by (case-insensitive, must not be blank)
     * @param createdAfter only include repositories created after this date (must not be in the future)
     * @return ranked list of popular repositories
     */
    @GetMapping("/repositories/popular")
    public List<RepositoryResponse> getPopularRepositories(
            @RequestParam @NotBlank String language,
            @RequestParam @PastOrPresent LocalDate createdAfter) {

        log.info("Fetching popular repositories for language={} createdAfter={}", language, createdAfter);

        List<RepositoryResponse> response = popularityService.getPopularRepositories(new SearchCriteria(language, createdAfter))
                .stream()
                .map(RepositoryResponse::from)
                .toList();

        log.info("Returning {} repositories for language={}", response.size(), language);

        return response;
    }
}
