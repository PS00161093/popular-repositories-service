package com.redcare.github.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * Configures the {@link RestClient} used to call the GitHub Search API.
 * Sets the base URL and Authorization header once at build time.
 */
@Configuration(proxyBeanMethods = false)
public class GitHubClientConfig {

    @Bean
    public RestClient gitHubRestClient(GithubProperties properties, RestClient.Builder restClientBuilder) {
        return restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .build();
    }
}
