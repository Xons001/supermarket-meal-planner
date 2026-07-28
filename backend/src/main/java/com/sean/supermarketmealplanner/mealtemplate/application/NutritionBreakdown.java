package com.sean.supermarketmealplanner.mealtemplate.application;

import java.math.BigDecimal;

public record NutritionBreakdown(
        BigDecimal calories,
        BigDecimal protein,
        BigDecimal carbohydrates,
        BigDecimal fat,
        BigDecimal fiber,
        BigDecimal sugar,
        BigDecimal salt
) {
}
