package com.guessv.dto;

public record UserProfileVO(
        String userId,
        String nickname,
        String gameId,
        String displayName,
        boolean isAnonymous,
        String username,
        String avatarUrl,
        String createdAt
) {}
