package com.sean.supermarketmealplanner.supermarket.application;

public record SupermarketResponse(
        String code,
        String name,
        boolean enabled,
        String catalogSource,
        String countryCode,
        String currencyCode
) {
}
