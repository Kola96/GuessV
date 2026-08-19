package com.guessv.dto;

public record GuessResponse(
        boolean correct,
        boolean gameOver,
        boolean win,
        int remainingAttempts,
        int attemptsUsed,
        ComparisonResult comparison,
        VtuberReveal targetVtuber
) {
    public record VtuberReveal(Long id, String name, String avatarUrl) {}
}
