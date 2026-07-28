package com.sean.supermarketmealplanner.catalog.application;

import java.math.BigDecimal;

public record NutritionPerUnitResponse(
        BigDecimal calories,
        BigDecimal protein,
        BigDecimal carbohydrates,
        BigDecimal fat,
        BigDecimal fiber,
        BigDecimal sugar,
        BigDecimal salt
) {
}
