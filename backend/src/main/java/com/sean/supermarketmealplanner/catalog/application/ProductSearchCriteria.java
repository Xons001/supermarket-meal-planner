package com.sean.supermarketmealplanner.catalog.application;

import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ProductSearchCriteria(
        SupermarketCode supermarketCode,
        UUID categoryId,
        String query,
        Boolean available,
        BigDecimal maximumPrice,
        BigDecimal maximumCalories,
        BigDecimal minimumProtein,
        Set<String> dietaryTags,
        Set<String> excludedAllergens
) {
}
