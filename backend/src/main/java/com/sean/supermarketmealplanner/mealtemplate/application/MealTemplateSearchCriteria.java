package com.sean.supermarketmealplanner.mealtemplate.application;

import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import java.math.BigDecimal;
import java.util.Set;

public record MealTemplateSearchCriteria(
        SupermarketCode supermarketCode,
        MealType mealType,
        Boolean active,
        String query,
        BigDecimal minimumProtein,
        BigDecimal maximumCalories,
        Integer maximumPreparationMinutes,
        Set<String> excludedAllergens,
        Set<String> dietaryTags,
        int page,
        int size,
        String sortField,
        boolean descending
) {
}
