package com.sean.supermarketmealplanner.catalog.application;

import java.util.UUID;

public record CatalogOptionResponse(UUID id, String code, String name) {
}
