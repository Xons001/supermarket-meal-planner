package com.sean.supermarketmealplanner.mealtemplate.infrastructure.bootstrap;

import java.math.BigDecimal;
import java.util.List;

public record MealTemplateSeedDocument(
        String classification,
        String supermarketCode,
        List<SeedTemplate> templates
) {
    public record SeedTemplate(
            String name,
            String description,
            String mealType,
            List<String> instructions,
            int preparationMinutes,
            int servings,
            boolean active,
            List<SeedIngredient> ingredients
    ) {
    }

    public record SeedIngredient(
            String productExternalId,
            BigDecimal quantity,
            String quantityUnit,
            boolean optional,
            int sortOrder,
            String notes
    ) {
    }
}
