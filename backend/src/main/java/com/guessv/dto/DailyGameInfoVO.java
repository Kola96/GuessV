package com.guessv.dto;

import java.util.List;

public record DailyGameInfoVO(
        String date,
        int maxAttempts,
        int totalVtuberCount,
        boolean hasPlayed,
        boolean hasWon,
        int attemptsUsed,
        List<GuessEntry> guesses
) {}
