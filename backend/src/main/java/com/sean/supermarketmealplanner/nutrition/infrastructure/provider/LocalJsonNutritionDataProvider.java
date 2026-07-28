package com.sean.supermarketmealplanner.nutrition.infrastructure.provider;

import com.sean.supermarketmealplanner.nutrition.application.port.ExternalNutritionData;
import com.sean.supermarketmealplanner.nutrition.application.port.NutritionDataProvider;
import com.sean.supermarketmealplanner.shared.infrastructure.demodata.DemoCatalogDocument.DemoNutrition;
import com.sean.supermarketmealplanner.shared.infrastructure.demodata.DemoCatalogFileReader;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LocalJsonNutritionDataProvider implements NutritionDataProvider {

    private final DemoCatalogFileReader fileReader;

    public LocalJsonNutritionDataProvider(DemoCatalogFileReader fileReader) {
        this.fileReader = fileReader;
    }

    @Override
    public Optional<ExternalNutritionData> findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return Optional.empty();
        }
        return fileReader.read().products().stream()
                .filter(product -> barcode.equalsIgnoreCase(product.barcode()))
                .findFirst()
                .map(product -> mapNutrition(product.nutrition()));
    }

    @Override
    public Optional<ExternalNutritionData> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return fileReader.read().products().stream()
                .filter(product -> name.equalsIgnoreCase(product.name()))
                .findFirst()
                .map(product -> mapNutrition(product.nutrition()));
    }

    private ExternalNutritionData mapNutrition(DemoNutrition nutrition) {
        return new ExternalNutritionData(
                nutrition.caloriesPer100g(),
                nutrition.proteinPer100g(),
                nutrition.carbohydratesPer100g(),
                nutrition.fatPer100g(),
                nutrition.fiberPer100g(),
                nutrition.sugarPer100g(),
                nutrition.saltPer100g(),
                nutrition.dataSource(),
                nutrition.verificationStatus(),
                nutrition.confidenceScore(),
                nutrition.updatedAt()
        );
    }
}
