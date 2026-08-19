package com.guessv.dto;

public record FieldComparison(Object value, String match, String direction) {
    public FieldComparison(Object value, String match) {
        this(value, match, null);
    }
}
