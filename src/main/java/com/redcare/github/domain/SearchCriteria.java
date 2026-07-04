package com.redcare.github.domain;

import java.time.LocalDate;

public record SearchCriteria(
        String language,
        LocalDate createdAfter
) {
    public SearchCriteria {
        language = language.toLowerCase();
    }
}
