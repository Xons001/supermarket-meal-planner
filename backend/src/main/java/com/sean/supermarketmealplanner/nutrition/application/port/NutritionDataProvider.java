package com.sean.supermarketmealplanner.nutrition.application.port;

import java.util.Optional;
import java.util.List;

public interface NutritionDataProvider {

    NutritionProviderCode supportedProvider();

    Optional<ExternalNutritionData> findByBarcode(String barcode);

    List<ExternalNutritionCandidate> searchByName(String name, NutritionSearchOptions options);

    default Optional<ExternalNutritionData> findByName(String name) {
        return searchByName(name, NutritionSearchOptions.defaults()).stream()
                .findFirst().map(ExternalNutritionCandidate::nutrition);
    }
}
