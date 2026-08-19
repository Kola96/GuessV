package com.guessv.dto;

public record ComparisonResult(
        FieldComparison name,
        FieldComparison region,
        FieldComparison group,
        FieldComparison debutYear,
        FieldComparison gender,
        FieldComparison status,
        FieldComparison hairColor,
        FieldComparison fanName
) {}
