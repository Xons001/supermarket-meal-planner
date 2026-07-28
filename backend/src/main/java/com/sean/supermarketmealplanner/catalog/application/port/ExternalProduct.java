package com.sean.supermarketmealplanner.catalog.application.port;

import com.sean.supermarketmealplanner.catalog.domain.PackageUnit;
import java.math.BigDecimal;

public record ExternalProduct(
        String externalId,
        String barcode,
        String categoryExternalId,
        String name,
        String brand,
        String description,
        BigDecimal packageQuantity,
        PackageUnit packageUnit,
        BigDecimal currentPrice,
        BigDecimal unitPrice,
        boolean available,
        String source
) {
}
