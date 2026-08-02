package com.sean.supermarketmealplanner.catalog.application;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductAllergenEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductDietaryTagEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.nutrition.infrastructure.persistence.NutritionEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductResponseMapper {

    public ProductResponse map(
            ProductEntity product,
            List<ProductDietaryTagEntity> dietaryTags,
            List<ProductAllergenEntity> allergens
    ) {
        return new ProductResponse(
                product.getId(),
                product.getSupermarket().getCode().name(),
                product.getSupermarket().getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getExternalId(),
                product.getBarcode(),
                product.getName(),
                product.getBrand(),
                product.getDescription(),
                product.getImageUrl(),
                product.getCurrentPrice(),
                product.getUnitPrice(),
                product.getPackageQuantity(),
                product.getPackageUnit().name(),
                product.getMeasurementType().name(),
                product.isCostDataComplete(),
                product.isAvailable(),
                product.getSource(),
                product.getLastSyncedAt(),
                mapNutrition(product.getNutrition()),
                dietaryTags.stream()
                        .map(relation -> new DietaryTagResponse(
                                relation.getDietaryTag().getId(),
                                relation.getDietaryTag().getCode(),
                                relation.getDietaryTag().getName()
                        ))
                        .sorted(java.util.Comparator.comparing(DietaryTagResponse::name))
                        .toList(),
                allergens.stream()
                        .map(relation -> new AllergenResponse(
                                relation.getAllergen().getId(),
                                relation.getAllergen().getCode(),
                                relation.getAllergen().getName(),
                                relation.getPresenceType().name()
                        ))
                        .sorted(java.util.Comparator.comparing(AllergenResponse::name))
                        .toList(),
                "DEMO_JSON".equals(product.getSource())
        );
    }

    private NutritionResponse mapNutrition(NutritionEntity nutrition) {
        if (nutrition == null) {
            return null;
        }
        return new NutritionResponse(
                nutrition.getCaloriesPer100g(),
                nutrition.getProteinPer100g(),
                nutrition.getCarbohydratesPer100g(),
                nutrition.getFatPer100g(),
                nutrition.getFiberPer100g(),
                nutrition.getSugarPer100g(),
                nutrition.getSaltPer100g(),
                nutrition.getSaturatedFatPer100g(),
                nutrition.getNutritionBasis(),
                nutrition.getCompleteness(),
                nutrition.getCaloriesPerUnit() == null ? null : new NutritionPerUnitResponse(
                        nutrition.getCaloriesPerUnit(),
                        nutrition.getProteinPerUnit(),
                        nutrition.getCarbohydratesPerUnit(),
                        nutrition.getFatPerUnit(),
                        nutrition.getFiberPerUnit(),
                        nutrition.getSugarPerUnit(),
                        nutrition.getSaltPerUnit()
                ),
                nutrition.getDataSource(),
                nutrition.getVerificationStatus(),
                nutrition.getConfidenceScore(),
                nutrition.getSourceReference(),
                nutrition.getSourceUpdatedAt(),
                nutrition.getUpdatedAt()
        );
    }
}
