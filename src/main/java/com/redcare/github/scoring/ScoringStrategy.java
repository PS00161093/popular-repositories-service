package com.redcare.github.scoring;

import com.redcare.github.client.GitHubRepository;

public interface ScoringStrategy {

    double score(GitHubRepository repository);
}
