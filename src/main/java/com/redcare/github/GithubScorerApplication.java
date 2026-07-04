package com.redcare.github;

import com.redcare.github.config.GithubProperties;
import com.redcare.github.config.ScoringProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GithubProperties.class, ScoringProperties.class})
public class GithubScorerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubScorerApplication.class, args);
    }
}
