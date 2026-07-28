package com.sean.supermarketmealplanner.catalog.application;

import org.springframework.data.domain.Pageable;

public record ParsedProductSearch(ProductSearchCriteria criteria, Pageable pageable) {
}
