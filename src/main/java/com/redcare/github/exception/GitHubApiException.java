package com.redcare.github.exception;

public class GitHubApiException extends RuntimeException {

    public GitHubApiException(String message) {
        super(message);
    }
}
