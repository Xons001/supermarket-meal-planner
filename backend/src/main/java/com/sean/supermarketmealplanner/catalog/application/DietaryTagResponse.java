package com.sean.supermarketmealplanner.catalog.application;

import java.util.UUID;

public record DietaryTagResponse(UUID id, String code, String name) {
}
