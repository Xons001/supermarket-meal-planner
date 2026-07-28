package com.sean.supermarketmealplanner.catalog.application.port;

import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import java.util.List;
import java.util.Optional;

public interface SupermarketCatalogProvider {

    SupermarketCode supportedSupermarket();

    List<ExternalCategory> fetchCategories();

    List<ExternalProduct> fetchProducts();

    Optional<ExternalProduct> fetchProduct(String externalId);
}
