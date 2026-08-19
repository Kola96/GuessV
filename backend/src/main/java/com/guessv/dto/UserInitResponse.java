package com.guessv.dto;

public record UserInitResponse(
        String userId,
        String nickname,
        String gameId,
        String displayName,
        String token,
        boolean isAnonymous
) {}
