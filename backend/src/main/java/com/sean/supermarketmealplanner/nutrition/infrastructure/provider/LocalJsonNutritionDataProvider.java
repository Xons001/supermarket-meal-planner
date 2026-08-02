package com.sean.supermarketmealplanner.nutrition.infrastructure.provider;

import com.sean.supermarketmealplanner.nutrition.application.port.ExternalNutritionData;
import com.sean.supermarketmealplanner.nutrition.application.port.ExternalUnitNutritionData;
import com.sean.supermarketmealplanner.nutrition.application.port.NutritionDataProvider;
import com.sean.supermarketmealplanner.shared.infrastructure.demodata.DemoCatalogDocument.DemoNutrition;
import com.sean.supermarketmealplanner.shared.infrastructure.demodata.DemoCatalogFileReader;
import java.util.Optional;
import java.util.List;
import com.sean.supermarketmealplanner.nutrition.application.port.*;
import org.springframework.stereotype.Component;

@Component
public class LocalJsonNutritionDataProvider implements NutritionDataProvider {

    private final DemoCatalogFileReader fileReader;

    public LocalJsonNutritionDataProvider(DemoCatalogFileReader fileReader) {
        this.fileReader = fileReader;
    }

    @Override
    public NutritionProviderCode supportedProvider() { return NutritionProviderCode.LOCAL_JSON; }

    @Override
    public Optional<ExternalNutritionData> findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return Optional.empty();
        }
        return fileReader.read().products().stream()
                .filter(product -> barcode.equalsIgnoreCase(product.barcode()))
                .filter(product -> product.nutrition() != null)
                .findFirst()
                .map(product -> mapNutrition(product.nutrition()));
    }

    @Override
    public List<ExternalNutritionCandidate> searchByName(String name, NutritionSearchOptions options) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return fileReader.read().products().stream()
                .filter(product -> normalize(product.name()).contains(normalize(name))
                        || normalize(name).contains(normalize(product.name())))
                .filter(product -> product.nutrition() != null)
                .limit(options.maximumResults())
                .map(product -> new ExternalNutritionCandidate(
                        "local-json:" + product.externalId(), product.barcode(), product.name(), product.brand(),
                        product.packageQuantity(), product.packageUnit(), product.categoryExternalId(),
                        mapNutrition(product.nutrition())))
                .toList();
    }

    private String normalize(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
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
                nutrition.perUnit() == null ? null : new ExternalUnitNutritionData(
                        nutrition.perUnit().calories(),
                        nutrition.perUnit().protein(),
                        nutrition.perUnit().carbohydrates(),
                        nutrition.perUnit().fat(),
                        nutrition.perUnit().fiber(),
                        nutrition.perUnit().sugar(),
                        nutrition.perUnit().salt()
                ),
                nutrition.dataSource(),
                nutrition.verificationStatus(),
                nutrition.confidenceScore(),
                nutrition.updatedAt()
        );
    }
}
