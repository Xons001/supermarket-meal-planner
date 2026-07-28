package com.sean.supermarketmealplanner.shared.infrastructure.demodata;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record DemoCatalogDocument(
        DemoMetadata metadata,
        String supermarketCode,
        List<DemoCategory> categories,
        List<DemoProduct> products
) {
    public record DemoMetadata(
            String datasetName,
            String classification,
            String priceDisclaimer,
            OffsetDateTime generatedAt
    ) {
    }

    public record DemoCategory(
            String externalId,
            String name,
            String parentExternalId
    ) {
    }

    public record DemoProduct(
            String externalId,
            String barcode,
            String categoryExternalId,
            String name,
            String brand,
            String description,
            BigDecimal packageQuantity,
            String packageUnit,
            BigDecimal currentPrice,
            BigDecimal unitPrice,
            boolean available,
            String source,
            List<String> allergens,
            List<String> dietaryTags,
            DemoNutrition nutrition
    ) {
    }

    public record DemoNutrition(
            BigDecimal caloriesPer100g,
            BigDecimal proteinPer100g,
            BigDecimal carbohydratesPer100g,
            BigDecimal fatPer100g,
            BigDecimal fiberPer100g,
            BigDecimal sugarPer100g,
            BigDecimal saltPer100g,
            String dataSource,
            String verificationStatus,
            BigDecimal confidenceScore,
            OffsetDateTime updatedAt
    ) {
    }
}
