package com.redcare.github.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("GitHub API rate limit exceeded");
    }
}
