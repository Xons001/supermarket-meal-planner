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
        ExternalUnitNutritionData perUnit,
        String dataSource,
        String verificationStatus,
        BigDecimal confidenceScore,
        OffsetDateTime updatedAt
) {
}
