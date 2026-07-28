package com.sean.supermarketmealplanner.catalog.application.port;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExternalPriceHistory(
        BigDecimal price,
        BigDecimal unitPrice,
        OffsetDateTime recordedAt
) {
}
