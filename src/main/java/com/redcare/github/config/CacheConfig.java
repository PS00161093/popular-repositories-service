package com.redcare.github.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's caching infrastructure.
 * Kept separate from the main application class so tests can exclude it to disable caching.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {
}
