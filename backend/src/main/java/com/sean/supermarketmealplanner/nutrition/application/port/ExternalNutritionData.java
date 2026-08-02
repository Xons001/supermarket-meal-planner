package com.sean.supermarketmealplanner.nutrition.application.port;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExternalNutritionData(
        BigDecimal caloriesPer100g,
        BigDecimal proteinPer100g,
        BigDecimal carbohydratesPer100g,
        BigDecimal fatPer100g,
        BigDecimal fiberPer100g,
        BigDecimal sugarPer100g,
        BigDecimal saltPer100g,
        BigDecimal saturatedFatPer100g,
        String nutritionBasis,
        ExternalUnitNutritionData perUnit,
        String dataSource,
        String verificationStatus,
        BigDecimal confidenceScore,
        String sourceReference,
        OffsetDateTime updatedAt
) {
    public ExternalNutritionData(BigDecimal caloriesPer100g, BigDecimal proteinPer100g,
            BigDecimal carbohydratesPer100g, BigDecimal fatPer100g, BigDecimal fiberPer100g,
            BigDecimal sugarPer100g, BigDecimal saltPer100g, ExternalUnitNutritionData perUnit,
            String dataSource, String verificationStatus, BigDecimal confidenceScore,
            OffsetDateTime updatedAt) {
        this(caloriesPer100g, proteinPer100g, carbohydratesPer100g, fatPer100g, fiberPer100g,
                sugarPer100g, saltPer100g, null, "PER_100_GRAMS", perUnit, dataSource,
                verificationStatus, confidenceScore, null, updatedAt);
    }
}
