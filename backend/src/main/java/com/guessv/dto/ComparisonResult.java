package com.guessv.dto;

public record ComparisonResult(
        FieldComparison name,
        FieldComparison region,
        FieldComparison group,
        FieldComparison debutYear,
        FieldComparison birthday,
        FieldComparison gender,
        FieldComparison status,
        FieldComparison hairColor,
        FieldComparison followerCount
) {}
