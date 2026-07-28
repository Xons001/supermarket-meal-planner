package com.sean.supermarketmealplanner.catalog.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PriceHistoryResponse(
        UUID id,
        BigDecimal price,
        BigDecimal unitPrice,
        OffsetDateTime recordedAt,
        boolean demonstrationData
) {
}
