package com.sean.supermarketmealplanner.nutrition.application.port;

import java.math.BigDecimal;

public record ExternalUnitNutritionData(
        BigDecimal calories,
        BigDecimal protein,
        BigDecimal carbohydrates,
        BigDecimal fat,
        BigDecimal fiber,
        BigDecimal sugar,
        BigDecimal salt
) {
}
