package com.guessv.dto;

public record VtuberSearchVO(
        Long id,
        String name,
        String nameCn,
        String nameEn,
        String avatarUrl,
        String groupName,
        String region
) {
}
