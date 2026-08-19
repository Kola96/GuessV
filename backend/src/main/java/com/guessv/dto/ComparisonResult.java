package com.guessv.dto;

public record ComparisonResult(
        FieldComparison name,
        FieldComparison platforms,
        FieldComparison group,
        FieldComparison debutYear,
        FieldComparison birthday,
        FieldComparison gender,
        FieldComparison status,
        FieldComparison hairColor,
        FieldComparison languages,
        FieldComparison followerCount
) {}
