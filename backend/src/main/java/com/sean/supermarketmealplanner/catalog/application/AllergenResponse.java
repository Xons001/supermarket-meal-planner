package com.sean.supermarketmealplanner.catalog.application;

import java.util.UUID;

public record AllergenResponse(
        UUID id,
        String code,
        String name,
        String presenceType
) {
}
