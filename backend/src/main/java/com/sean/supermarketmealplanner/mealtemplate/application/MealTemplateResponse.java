package com.sean.supermarketmealplanner.mealtemplate.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record MealTemplateResponse(
        UUID id,
        String supermarketCode,
        String supermarketName,
        String name,
        String description,
        String mealType,
        List<String> instructions,
        int preparationMinutes,
        int servings,
        boolean active,
        String imageUrl,
        List<MealTemplateIngredientResponse> ingredients,
        NutritionBreakdown totalNutrition,
        NutritionBreakdown nutritionPerServing,
        BigDecimal totalConsumedCost,
        BigDecimal consumedCostPerServing,
        boolean calculationComplete,
        boolean nutritionComplete,
        boolean costComplete,
        List<String> warnings,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean demoData
) {
    public MealTemplateResponse withWarnings(List<String> additionalWarnings) {
        if (additionalWarnings.isEmpty()) {
            return this;
        }
        var combined = new ArrayList<>(warnings);
        additionalWarnings.stream().filter(warning -> !combined.contains(warning)).forEach(combined::add);
        return new MealTemplateResponse(
                id,
                supermarketCode,
                supermarketName,
                name,
                description,
                mealType,
                instructions,
                preparationMinutes,
                servings,
                active,
                imageUrl,
                ingredients,
                totalNutrition,
                nutritionPerServing,
                totalConsumedCost,
                consumedCostPerServing,
                calculationComplete,
                nutritionComplete,
                costComplete,
                List.copyOf(combined),
                createdAt,
                updatedAt,
                demoData
        );
    }
}
