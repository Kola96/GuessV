package com.guessv.dto;

public record NicknameCheckResponse(
        boolean valid,
        String reason
) {}
