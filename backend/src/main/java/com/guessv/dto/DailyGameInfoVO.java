package com.guessv.dto;

import java.util.List;

public record DailyGameInfoVO(
        String date,
        int maxAttempts,
        int totalVtuberCount,
        boolean hasPlayed,
        boolean hasWon,
        int attemptsUsed,
        List<GuessEntry> guesses,
        // 仅在游戏已结束时返回（防作弊：游戏进行中不暴露目标）
        String targetName,
        String targetAvatarUrl
) {}
