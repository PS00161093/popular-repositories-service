package com.redcare.github.client;

import java.util.List;

public record GitHubSearchResult(List<GitHubRepository> repositories, boolean partial) {
}
