package com.guessv.dto;

public record GuessEntry(
        Long vtuberId,
        String vtuberName,
        int attemptNumber,
        boolean correct,
        ComparisonResult comparison,
        String guessedAt
) {}
