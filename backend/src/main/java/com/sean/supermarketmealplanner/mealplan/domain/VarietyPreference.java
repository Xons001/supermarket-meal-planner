package com.sean.supermarketmealplanner.mealplan.domain;

public enum VarietyPreference {
    LOW,
    MEDIUM,
    HIGH;

    public int defaultMaximumRepetitions() {
        return switch (this) {
            case HIGH -> 2;
            case MEDIUM -> 3;
            case LOW -> 4;
        };
    }

    public java.math.BigDecimal targetUniqueRatio() {
        return switch (this) {
            case HIGH -> new java.math.BigDecimal("0.70");
            case MEDIUM -> new java.math.BigDecimal("0.50");
            case LOW -> new java.math.BigDecimal("0.30");
        };
    }
}
