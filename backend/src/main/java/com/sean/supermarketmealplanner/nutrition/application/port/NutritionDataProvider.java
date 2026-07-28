package com.sean.supermarketmealplanner.nutrition.application.port;

import java.util.Optional;

public interface NutritionDataProvider {

    Optional<ExternalNutritionData> findByBarcode(String barcode);

    Optional<ExternalNutritionData> findByName(String name);
}
