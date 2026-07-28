package com.sean.supermarketmealplanner.catalog.application.port;

import com.sean.supermarketmealplanner.catalog.domain.PackageUnit;
import com.sean.supermarketmealplanner.catalog.domain.MeasurementType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record ExternalProduct(
        String externalId,
        String barcode,
        String categoryExternalId,
        String name,
        String brand,
        String description,
        BigDecimal packageQuantity,
        PackageUnit packageUnit,
        MeasurementType measurementType,
        boolean costDataComplete,
        BigDecimal currentPrice,
        BigDecimal unitPrice,
        boolean available,
        String source,
        List<ExternalAllergen> allergens,
        Set<String> dietaryTags,
        List<ExternalPriceHistory> priceHistory
) {
}
