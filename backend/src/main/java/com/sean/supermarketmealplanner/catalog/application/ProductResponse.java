package com.sean.supermarketmealplanner.catalog.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String supermarketCode,
        String supermarketName,
        UUID categoryId,
        String categoryName,
        String externalId,
        String barcode,
        String name,
        String brand,
        String description,
        BigDecimal currentPrice,
        BigDecimal unitPrice,
        BigDecimal packageQuantity,
        String packageUnit,
        boolean available,
        String source,
        OffsetDateTime lastSyncedAt,
        NutritionResponse nutrition,
        boolean demonstrationData
) {
}
