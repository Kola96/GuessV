package com.guessv.dto;

import jakarta.validation.constraints.Size;

public record UserInitRequest(
        @Size(min = 2, max = 16) String nickname,
        Boolean useRandomNickname,
        String deviceFingerprint
) {}
