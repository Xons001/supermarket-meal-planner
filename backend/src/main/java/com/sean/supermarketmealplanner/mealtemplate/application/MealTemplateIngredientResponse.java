package com.sean.supermarketmealplanner.mealtemplate.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MealTemplateIngredientResponse(
        UUID productId,
        String productName,
        String brand,
        String category,
        BigDecimal quantity,
        String quantityUnit,
        boolean optional,
        int sortOrder,
        String notes,
        NutritionBreakdown calculatedNutrition,
        BigDecimal calculatedConsumedCost,
        boolean nutritionCalculationComplete,
        boolean costCalculationComplete,
        boolean calculationComplete,
        List<String> warnings
) {
}
