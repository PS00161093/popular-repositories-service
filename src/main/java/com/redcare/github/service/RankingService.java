package com.redcare.github.service;

import com.redcare.github.domain.PopularRepository;
import com.redcare.github.domain.ScoredRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Sorts scored repositories by popularity score descending and assigns a 1-based rank to each.
 */
@Service
public class RankingService {

    private static final Logger log = LoggerFactory.getLogger(RankingService.class);

    /**
     * Ranks the given scored repositories by popularity score descending.
     *
     * @param scored repositories with computed popularity scores
     * @return repositories sorted by score descending with a 1-based rank assigned
     */
    public List<PopularRepository> rank(List<ScoredRepository> scored) {
        log.debug("Ranking {} scored repositories", scored.size());

        List<ScoredRepository> sorted = scored.stream()
                .sorted(Comparator.comparingDouble(ScoredRepository::popularityScore).reversed())
                .toList();

        List<PopularRepository> ranked = IntStream.range(0, sorted.size())
                .mapToObj(i -> PopularRepository.from(sorted.get(i), i + 1))
                .toList();

        if (!ranked.isEmpty()) {
            log.debug("Top ranked repository: name={} score={}", ranked.getFirst().name(), ranked.getFirst().popularityScore());
        }
        return ranked;
    }
}
