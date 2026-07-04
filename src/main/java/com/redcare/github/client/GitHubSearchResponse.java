package com.redcare.github.client;

import java.util.List;

public record GitHubSearchResponse(
        boolean incompleteResults,
        List<GitHubRepository> items
) {
}
