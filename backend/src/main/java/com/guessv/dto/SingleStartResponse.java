package com.guessv.dto;

public record SingleStartResponse(Long sessionId, int maxAttempts, String poolTag, int vtuberCount) {}
