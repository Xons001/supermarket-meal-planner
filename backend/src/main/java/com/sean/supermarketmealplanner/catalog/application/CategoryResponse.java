package com.sean.supermarketmealplanner.catalog.application;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String externalId,
        String name,
        UUID parentCategoryId,
        String supermarketCode
) {
}
