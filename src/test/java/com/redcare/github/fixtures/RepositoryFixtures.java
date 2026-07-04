package com.redcare.github.fixtures;

import com.redcare.github.client.GitHubRepository;
import com.redcare.github.domain.PopularRepository;
import com.redcare.github.domain.ScoredRepository;

import java.time.Instant;

public final class RepositoryFixtures {

    public static final GitHubRepository HELLO_ALGO = new GitHubRepository(
            561730219L, "hello-algo", "krahets/hello-algo",
            "https://github.com/krahets/hello-algo", 128141, 15260,
            Instant.parse("2026-04-18T18:23:33Z"));

    public static final GitHubRepository STIRLING_PDF = new GitHubRepository(
            594155488L, "Stirling-PDF", "Stirling-Tools/Stirling-PDF",
            "https://github.com/Stirling-Tools/Stirling-PDF", 86049, 7525,
            Instant.parse("2026-07-04T15:41:19Z"));

    public static final GitHubRepository CONDUCTOR = new GitHubRepository(
            728981719L, "conductor", "conductor-oss/conductor",
            "https://github.com/conductor-oss/conductor", 31993, 949,
            Instant.parse("2026-07-03T19:26:51Z"));

    public static final GitHubRepository SMART_TUBE = new GitHubRepository(
            283211901L, "SmartTube", "yuliskov/SmartTube",
            "https://github.com/yuliskov/SmartTube", 30940, 1673,
            Instant.parse("2026-07-03T16:01:57Z"));

    public static final GitHubRepository HELLO_ALGORITHM = new GitHubRepository(
            267775629L, "hello-algorithm", "geekxh/hello-algorithm",
            "https://github.com/geekxh/hello-algorithm", 36080, 6415,
            Instant.parse("2023-06-13T04:13:17Z"));

    public static final GitHubRepository LANGCHAIN4J = new GitHubRepository(
            656264456L, "langchain4j", "langchain4j/langchain4j",
            "https://github.com/langchain4j/langchain4j", 12515, 2345,
            Instant.parse("2026-07-02T08:28:42Z"));

    public static final GitHubRepository SPRING_AI = new GitHubRepository(
            659402878L, "spring-ai", "spring-projects/spring-ai",
            "https://github.com/spring-projects/spring-ai", 9062, 2696,
            Instant.parse("2026-07-03T15:19:39Z"));

    public static final GitHubRepository SPRING_AI_ALIBABA = new GitHubRepository(
            854337508L, "spring-ai-alibaba", "alibaba/spring-ai-alibaba",
            "https://github.com/alibaba/spring-ai-alibaba", 10215, 2261,
            Instant.parse("2026-07-03T14:42:05Z"));

    public static final ScoredRepository SCORED_HELLO_ALGO = new ScoredRepository(
            561730219L, "hello-algo", "krahets/hello-algo",
            "https://github.com/krahets/hello-algo", 128141, 15260,
            Instant.parse("2026-04-18T18:23:33Z"), 4.37);

    public static final ScoredRepository SCORED_STIRLING_PDF = new ScoredRepository(
            594155488L, "Stirling-PDF", "Stirling-Tools/Stirling-PDF",
            "https://github.com/Stirling-Tools/Stirling-PDF", 86049, 7525,
            Instant.parse("2026-07-04T15:41:19Z"), 4.12);

    public static final ScoredRepository SCORED_CONDUCTOR = new ScoredRepository(
            728981719L, "conductor", "conductor-oss/conductor",
            "https://github.com/conductor-oss/conductor", 31993, 949,
            Instant.parse("2026-07-03T19:26:51Z"), 3.51);

    public static final ScoredRepository SCORED_HELLO_ALGORITHM = new ScoredRepository(
            267775629L, "hello-algorithm", "geekxh/hello-algorithm",
            "https://github.com/geekxh/hello-algorithm", 36080, 6415,
            Instant.parse("2023-06-13T04:13:17Z"), 2.83);

    public static final PopularRepository RANKED_HELLO_ALGO = new PopularRepository(
            561730219L, "hello-algo", "krahets/hello-algo",
            "https://github.com/krahets/hello-algo", 128141, 15260,
            Instant.parse("2026-04-18T18:23:33Z"), 4.37, 1);

    public static final PopularRepository RANKED_STIRLING_PDF = new PopularRepository(
            594155488L, "Stirling-PDF", "Stirling-Tools/Stirling-PDF",
            "https://github.com/Stirling-Tools/Stirling-PDF", 86049, 7525,
            Instant.parse("2026-07-04T15:41:19Z"), 4.12, 2);

    private RepositoryFixtures() {
    }
}
