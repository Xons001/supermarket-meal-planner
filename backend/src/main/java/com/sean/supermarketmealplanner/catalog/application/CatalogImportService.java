package com.sean.supermarketmealplanner.catalog.application;

import com.sean.supermarketmealplanner.catalog.application.port.ExternalProduct;
import com.sean.supermarketmealplanner.catalog.application.port.SupermarketCatalogProvider;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.CategoryEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.CategoryRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
import com.sean.supermarketmealplanner.nutrition.application.port.NutritionDataProvider;
import com.sean.supermarketmealplanner.nutrition.infrastructure.persistence.NutritionEntity;
import com.sean.supermarketmealplanner.nutrition.infrastructure.persistence.NutritionRepository;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogImportService {

    private final List<SupermarketCatalogProvider> catalogProviders;
    private final NutritionDataProvider nutritionDataProvider;
    private final SupermarketRepository supermarketRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final NutritionRepository nutritionRepository;

    public CatalogImportService(
            List<SupermarketCatalogProvider> catalogProviders,
            NutritionDataProvider nutritionDataProvider,
            SupermarketRepository supermarketRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            NutritionRepository nutritionRepository
    ) {
        this.catalogProviders = catalogProviders;
        this.nutritionDataProvider = nutritionDataProvider;
        this.supermarketRepository = supermarketRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.nutritionRepository = nutritionRepository;
    }

    @Transactional
    public void importCatalogs() {
        catalogProviders.forEach(this::importCatalog);
    }

    private void importCatalog(SupermarketCatalogProvider provider) {
        var supermarket = supermarketRepository.findByCode(provider.supportedSupermarket())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing supermarket seed: " + provider.supportedSupermarket()
                ));
        if (!supermarket.isEnabled()) {
            return;
        }

        Map<String, CategoryEntity> categoriesByExternalId = new HashMap<>();
        for (var externalCategory : provider.fetchCategories()) {
            requireText(externalCategory.externalId(), "Category external id");
            requireText(externalCategory.name(), "Category name");
            var category = categoryRepository
                    .findBySupermarketIdAndExternalId(supermarket.getId(), externalCategory.externalId())
                    .orElseGet(() -> new CategoryEntity(
                            supermarket,
                            externalCategory.externalId(),
                            externalCategory.name()
                    ));
            category.update(externalCategory.name(), null, true);
            categoriesByExternalId.put(externalCategory.externalId(), categoryRepository.save(category));
        }

        for (var externalCategory : provider.fetchCategories()) {
            if (externalCategory.parentExternalId() != null) {
                var category = categoriesByExternalId.get(externalCategory.externalId());
                var parent = categoriesByExternalId.get(externalCategory.parentExternalId());
                if (parent == null) {
                    throw new IllegalStateException(
                            "Unknown parent category: " + externalCategory.parentExternalId()
                    );
                }
                category.update(externalCategory.name(), parent, true);
            }
        }

        Set<String> importedExternalIds = new HashSet<>();
        var syncedAt = OffsetDateTime.now();
        for (var externalProduct : provider.fetchProducts()) {
            validateProduct(externalProduct);
            var category = categoriesByExternalId.get(externalProduct.categoryExternalId());
            if (category == null) {
                throw new IllegalStateException(
                        "Unknown product category: " + externalProduct.categoryExternalId()
                );
            }

            var product = productRepository
                    .findBySupermarketIdAndExternalId(supermarket.getId(), externalProduct.externalId())
                    .orElseGet(() -> new ProductEntity(
                            supermarket,
                            category,
                            externalProduct.externalId()
                    ));
            product.update(
                    category,
                    externalProduct.barcode(),
                    externalProduct.name(),
                    externalProduct.brand(),
                    externalProduct.description(),
                    externalProduct.currentPrice(),
                    externalProduct.unitPrice(),
                    externalProduct.packageQuantity(),
                    externalProduct.packageUnit(),
                    externalProduct.available(),
                    externalProduct.source(),
                    syncedAt
            );
            productRepository.save(product);
            importedExternalIds.add(externalProduct.externalId());
            importNutrition(product, externalProduct);
        }

        productRepository.findAllBySupermarketId(supermarket.getId()).stream()
                .filter(product -> !importedExternalIds.contains(product.getExternalId()))
                .forEach(product -> product.markUnavailable(syncedAt));
    }

    private void importNutrition(ProductEntity product, ExternalProduct externalProduct) {
        var externalNutrition = nutritionDataProvider.findByBarcode(externalProduct.barcode())
                .or(() -> nutritionDataProvider.findByName(externalProduct.name()))
                .orElseThrow(() -> new IllegalStateException(
                        "Missing nutrition data for product: " + externalProduct.externalId()
                ));
        var nutrition = nutritionRepository.findByProductId(product.getId())
                .orElseGet(() -> new NutritionEntity(product));
        nutrition.update(
                externalNutrition.caloriesPer100g(),
                externalNutrition.proteinPer100g(),
                externalNutrition.carbohydratesPer100g(),
                externalNutrition.fatPer100g(),
                externalNutrition.fiberPer100g(),
                externalNutrition.sugarPer100g(),
                externalNutrition.saltPer100g(),
                externalNutrition.dataSource(),
                externalNutrition.verificationStatus(),
                externalNutrition.confidenceScore(),
                externalNutrition.updatedAt()
        );
        nutritionRepository.save(nutrition);
        product.setNutrition(nutrition);
    }

    private void validateProduct(ExternalProduct product) {
        requireText(product.externalId(), "Product external id");
        requireText(product.name(), "Product name");
        requireText(product.categoryExternalId(), "Product category");
        requireNonNegative(product.currentPrice(), "Current price");
        requireNonNegative(product.unitPrice(), "Unit price");
        if (product.packageQuantity() == null || product.packageQuantity().signum() <= 0) {
            throw new IllegalArgumentException("Package quantity must be positive");
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private void requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }
}
