package com.sean.supermarketmealplanner.catalog.application.port;

public record ExternalCategory(
        String externalId,
        String name,
        String parentExternalId
) {
}
