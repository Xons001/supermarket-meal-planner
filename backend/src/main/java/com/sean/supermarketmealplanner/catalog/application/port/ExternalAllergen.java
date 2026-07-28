package com.sean.supermarketmealplanner.catalog.application.port;

import com.sean.supermarketmealplanner.catalog.domain.PresenceType;

public record ExternalAllergen(String code, PresenceType presenceType) {
}
