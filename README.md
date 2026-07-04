# GitHub Repository Scorer

## Overview

This service implements the brief requirement: given a programming language and an earliest creation date, return GitHub repositories ranked by a computed popularity score.

It accepts `language` and `createdAfter` as query parameters, fetches the top 100 repositories from the GitHub Search API, scores each one using a weighted formula that combines stars, forks, and recency of the last push, and returns them sorted by popularity score descending. Each result includes the `popularity_score` and `rank` so the algorithm's output is transparent and verifiable.

The scoring formula is designed to surface actively maintained, widely adopted repositories - not just those with the highest raw star count. A repository with 3,000 stars, many forks, and a push yesterday can rank above one with 5,000 stars and no recent activity.

Results are cached for 12 hours per `(language, createdAfter)` pair - star and fork counts on popular repositories change slowly, so a long TTL is safe and reduces unnecessary API calls. GitHub API failures are handled with retry and circuit breaker protection.

---

## Assumptions

- **Authentication**: This service exposes a public API - no authentication or authorisation is required to call it.
- **GitHub token scope**: A classic PAT with no scopes (or a fine-grained token with public repository read access) is sufficient. The search API only queries public repositories.
- **Result set**: Customers care about the most popular repositories - not an exhaustive list. The top 100 by stars is sufficient for this use case. This service fetches them in a single request (`per_page=100`, `sort=stars`, `order=desc`). Paginating through thousands of low-star repositories to re-rank them locally adds no value and wastes API quota.
- **Result ordering**: GitHub's search API returns results sorted by stars. This service re-ranks them by the computed popularity score (which factors in forks and recency in addition to stars), so the final order may differ slightly.
- **`pushed_at` nullability**: The GitHub API defines `pushed_at` as `string or null`. Repositories that have never had a push event (e.g. freshly initialised via the UI with no commits) receive `recencyScore = 0.0` and are not penalised beyond losing the recency contribution.
- **`createdAfter` boundary**: The query uses `created:>date` (exclusive). A repository created exactly on `createdAfter` is excluded. This matches the parameter name semantics.
- **Language matching**: GitHub performs language matching on its side. Passing `java` and `Java` produce the same results. The service normalises to lowercase before the query so both spellings share a cache entry.
- **Empty results**: If no repositories match the query, the service returns `200 OK` with `[]`.

---

## Decisions

### Scoring formula

```
popularity_score = (0.6 × log10(stars + 1))
                 + (0.3 × log10(forks + 1))
                 + (0.1 × recencyScore)

recencyScore = 1.0 / (daysSinceLastPush + 1)
```

**Stars - 60% weight**

Stars are the most universally recognised signal of popularity - they directly express how many developers found a repository valuable enough to bookmark. This gets the highest weight. `log10` dampens the scale so a repository with 100k stars does not completely dwarf one with 10k. `+1` prevents `log(0)`.

| Stars | log10(stars+1) | Weighted (×0.6) |
|-------|----------------|-----------------|
| 0 | 0.0 | 0.0 |
| 100 | 2.0 | 1.2 |
| 1,000 | 3.0 | 1.8 |
| 10,000 | 4.0 | 2.4 |
| 100,000 | 5.0 | 3.0 |

**Forks - 30% weight**

Forks signal active adoption - developers forking to contribute, extend, or build on top of the project. A high fork count alongside high stars is a strong indicator of a healthy, actively used repository. Same log10 dampening applies.

**Recency - 10% weight**

A secondary tiebreaker that rewards actively maintained repositories. `1.0 / (daysSinceLastPush + 1)` decays rapidly. Its max contribution is `0.1`, so it cannot overcome a meaningful star or fork gap - it only differentiates repositories that are otherwise close in score.

| Last pushed | Recency score | Weighted (×0.1) |
|-------------|---------------|-----------------|
| Today | 1.0 | 0.10 |
| 1 day ago | 0.5 | 0.05 |
| 9 days ago | 0.1 | 0.01 |
| 99 days ago | 0.01 | 0.001 |

All weights are configurable in `application.yml` under `scoring.weights`. They must be positive and sum to exactly 1.0 — the application will refuse to start otherwise.

**Recency proxy**

`pushed_at` is used as the recency signal. It is the strongest available proxy from the GitHub Search API without additional per-repository calls. `updated_at` was considered but rejected — it includes non-code activity such as wiki edits, issue label changes, and project board updates, which do not reflect active development.

**Example**

| Repo | Stars | Forks | Last push | Score |
|------|-------|-------|-----------|-------|
| A | 5000 | 400 | 2 days ago | ≈ 3.04 |
| B | 3000 | 800 | today | ≈ 3.05 |

B edges out A despite fewer stars due to more forks and a fresh push.

### Caching

The GitHub API call is expensive - it counts against rate limits and adds latency on every cache miss. Raw results are therefore cached keyed on `(language, createdAfter)` for 12 hours (Caffeine, max 500 entries).

A 12-hour TTL is appropriate because star and fork counts on popular repositories change slowly - the relative ranking of the top 100 is stable over hours, not minutes. A shorter TTL would burn API quota without meaningfully improving result freshness.

The 500-entry cap is a memory safety ceiling, not an expected steady state. Each entry holds up to 100 repositories at roughly 300 bytes each, so 500 entries would consume at most ~15 MB. In practice, with a limited set of languages and common date ranges, real usage will be well under 100 entries.

Partial responses (`incomplete_results: true`) are never cached - the next request will attempt a full fetch. Scoring and ranking run on every cache hit and miss; they are cheap CPU operations. Caching the raw `GitHubRepository` list rather than the scored results means recency scores (`daysSinceLastPush`) are recomputed on every call and stay fresh throughout the 12-hour window — a repository pushed yesterday is not frozen as "pushed yesterday" for 12 hours.

`@Cacheable` was intentionally avoided in favour of programmatic `CacheManager` access to enable the conditional caching logic on `incomplete_results`.

### Resilience

- **Retry**: 3 attempts with 500ms initial wait and 2x exponential backoff. `RateLimitExceededException` is excluded - retrying a rate-limited call would immediately hit 403 again. `GitHubApiException` and `RateLimitExceededException` intentionally share no inheritance so their retry/circuit-breaker configurations remain independent.
- **Circuit breaker**: Opens after 50% failure rate over a 10-request sliding window. Stays open for 30 seconds before attempting recovery. When open, requests fail immediately with `503` rather than waiting for timeouts.

### Service decomposition

The pipeline is split into four focused services:
- `GitHubSearchService` - owns the cache and calls the API client
- `ScoringService` - pure transformation: `List<GitHubRepository>` → `List<ScoredRepository>`
- `RankingService` - pure transformation: `List<ScoredRepository>` → `List<PopularRepository>`
- `PopularityService` - orchestrates the three above; the only entry point from the controller

---

## Stack

- Java 21, Spring Boot 3.5
- Virtual threads (`spring.threads.virtual.enabled=true`)
- Caffeine cache, Resilience4j (retry + circuit breaker)
- RFC 7807 Problem Details error responses
- Spring Boot Actuator (`/actuator/health`, `/actuator/info`)

## Prerequisites

- Java 21
- A GitHub personal access token (classic PAT with no scopes, or a fine-grained token with public repository read access)

## How to Run

```bash
export GITHUB_TOKEN=your_github_token
./gradlew bootRun
```

The application will refuse to start if `GITHUB_TOKEN` is absent or blank.

## Running Tests

```bash
./gradlew test
```

---

## API

### GET /api/v1/repositories/popular

Returns repositories matching the given language and creation filter, ranked by popularity score descending.

**Query parameters**

| Parameter      | Type            | Required | Description                                     |
|----------------|-----------------|----------|-------------------------------------------------|
| `language`     | string          | yes      | Programming language (case-insensitive)         |
| `createdAfter` | date (ISO 8601) | yes      | Only include repos created after this date      |

**Sample request**

```http
GET /api/v1/repositories/popular?language=java&createdAfter=2024-01-01
Accept: application/json
```

**Sample response - 200 OK**

```json
[
  {
    "id": 123456789,
    "name": "awesome-java",
    "full_name": "acme/awesome-java",
    "html_url": "https://github.com/acme/awesome-java",
    "stars": 12400,
    "forks": 980,
    "pushed_at": "2024-11-03T14:22:00Z",
    "popularity_score": 3.87,
    "rank": 1
  },
  {
    "id": 987654321,
    "name": "java-toolkit",
    "full_name": "acme/java-toolkit",
    "html_url": "https://github.com/acme/java-toolkit",
    "stars": 8750,
    "forks": 1120,
    "pushed_at": "2024-10-28T09:10:00Z",
    "popularity_score": 3.62,
    "rank": 2
  }
]
```

**Error responses**

| Condition              | Status                  | Detail                                                             |
|------------------------|-------------------------|--------------------------------------------------------------------|
| Blank `language`       | `400 Bad Request`       | Constraint violation                                               |
| Future `createdAfter`  | `400 Bad Request`       | Constraint violation                                               |
| GitHub rate limit hit  | `429 Too Many Requests` | You are rate limited. Please try after some time.                  |
| Circuit breaker open   | `503 Service Unavailable` | Unable to get popular repositories. Please try after some time.  |
| GitHub API error       | `503 Service Unavailable` | Unable to get popular repositories. Please try after some time.  |

---

## GitHub Search API Contract

This service calls the [GitHub Search Repositories API](https://docs.github.com/en/rest/search/search?apiVersion=2022-11-28#search-repositories).

**Outbound request**

```http
GET https://api.github.com/search/repositories?q=language:java+created:>2024-01-01&per_page=100&sort=stars&order=desc
Accept: application/vnd.github+json
Authorization: Bearer <token>
X-GitHub-Api-Version: 2022-11-28
```

**GitHub response structure**

```json
{
  "incomplete_results": false,
  "items": [
    {
      "id": 123456789,
      "name": "awesome-java",
      "full_name": "acme/awesome-java",
      "html_url": "https://github.com/acme/awesome-java",
      "pushed_at": "2024-11-03T14:22:00Z",
      "stargazers_count": 12400,
      "forks_count": 980
    }
  ]
}
```

Fields consumed by this service: `incomplete_results`, `items[].id`, `items[].name`, `items[].full_name`, `items[].html_url`, `items[].stargazers_count`, `items[].forks_count`, `items[].pushed_at`. All other fields are ignored.

---

## Architecture

```
PopularityController
    └── PopularityService          (orchestrator)
            ├── GitHubSearchService    (cache layer + partial-result guard)
            │       └── GitHubClient   (@Retry + @CircuitBreaker, top 100 by stars)
            ├── ScoringService         (assigns popularity score per repo)
            └── RankingService         (sorts by score, assigns 1-based rank)
```

**Package layout**

```
com.redcare.github
├── client        GitHubClient, GitHubRepository, GitHubSearchResponse, GitHubSearchResult
├── config        GithubProperties, ScoringProperties, GitHubClientConfig, CacheConfig, ClockConfig
├── controller    PopularityController, RepositoryResponse
├── domain        SearchCriteria, ScoredRepository, PopularRepository
├── exception     GitHubApiException, RateLimitExceededException, GlobalExceptionHandler
├── scoring       ScoringStrategy (interface), WeightedPopularityScorer
└── service       PopularityService, GitHubSearchService, ScoringService, RankingService
```
