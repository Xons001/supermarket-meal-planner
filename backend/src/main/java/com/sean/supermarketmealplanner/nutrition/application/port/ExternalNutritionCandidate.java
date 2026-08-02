package com.sean.supermarketmealplanner.nutrition.application.port;

import java.math.BigDecimal;

public record ExternalNutritionCandidate(
        String externalReference,
        String barcode,
        String name,
        String brand,
        BigDecimal packageQuantity,
        String packageUnit,
        String category,
        ExternalNutritionData nutrition
) {}
