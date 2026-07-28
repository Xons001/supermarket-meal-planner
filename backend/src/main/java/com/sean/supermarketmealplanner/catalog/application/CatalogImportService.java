package com.sean.supermarketmealplanner.catalog.application;

import com.sean.supermarketmealplanner.catalog.application.port.ExternalProduct;
import com.sean.supermarketmealplanner.catalog.application.port.SupermarketCatalogProvider;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.CategoryEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.CategoryRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.AllergenRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.DietaryTagRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductAllergenEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductAllergenRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductDietaryTagEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductDietaryTagRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductPriceHistoryEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductPriceHistoryRepository;
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
    private final DietaryTagRepository dietaryTagRepository;
    private final AllergenRepository allergenRepository;
    private final ProductDietaryTagRepository productDietaryTagRepository;
    private final ProductAllergenRepository productAllergenRepository;
    private final ProductPriceHistoryRepository priceHistoryRepository;

    public CatalogImportService(
            List<SupermarketCatalogProvider> catalogProviders,
            NutritionDataProvider nutritionDataProvider,
            SupermarketRepository supermarketRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            NutritionRepository nutritionRepository,
            DietaryTagRepository dietaryTagRepository,
            AllergenRepository allergenRepository,
            ProductDietaryTagRepository productDietaryTagRepository,
            ProductAllergenRepository productAllergenRepository,
            ProductPriceHistoryRepository priceHistoryRepository
    ) {
        this.catalogProviders = catalogProviders;
        this.nutritionDataProvider = nutritionDataProvider;
        this.supermarketRepository = supermarketRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.nutritionRepository = nutritionRepository;
        this.dietaryTagRepository = dietaryTagRepository;
        this.allergenRepository = allergenRepository;
        this.productDietaryTagRepository = productDietaryTagRepository;
        this.productAllergenRepository = productAllergenRepository;
        this.priceHistoryRepository = priceHistoryRepository;
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
        categoryRepository.findAllBySupermarketId(supermarket.getId()).stream()
                .filter(category -> !categoriesByExternalId.containsKey(category.getExternalId()))
                .forEach(category -> category.update(
                        category.getName(),
                        category.getParentCategory(),
                        false
                ));

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
            importDietaryTags(product, externalProduct);
            importAllergens(product, externalProduct);
            importPriceHistory(product, externalProduct);
        }

        productRepository.findAllBySupermarketId(supermarket.getId()).stream()
                .filter(product -> !importedExternalIds.contains(product.getExternalId()))
                .forEach(product -> product.markUnavailable(syncedAt));
    }

    private void importNutrition(ProductEntity product, ExternalProduct externalProduct) {
        var externalNutrition = nutritionDataProvider.findByBarcode(externalProduct.barcode())
                .or(() -> nutritionDataProvider.findByName(externalProduct.name()));
        if (externalNutrition.isEmpty()) {
            nutritionRepository.findByProductId(product.getId()).ifPresent(nutritionRepository::delete);
            product.setNutrition(null);
            return;
        }
        var nutrition = nutritionRepository.findByProductId(product.getId())
                .orElseGet(() -> new NutritionEntity(product));
        var nutritionData = externalNutrition.orElseThrow();
        nutrition.update(
                nutritionData.caloriesPer100g(),
                nutritionData.proteinPer100g(),
                nutritionData.carbohydratesPer100g(),
                nutritionData.fatPer100g(),
                nutritionData.fiberPer100g(),
                nutritionData.sugarPer100g(),
                nutritionData.saltPer100g(),
                nutritionData.dataSource(),
                nutritionData.verificationStatus(),
                nutritionData.confidenceScore(),
                nutritionData.updatedAt()
        );
        nutritionRepository.save(nutrition);
        product.setNutrition(nutrition);
    }

    private void importDietaryTags(ProductEntity product, ExternalProduct externalProduct) {
        productDietaryTagRepository.deleteAllByProductId(product.getId());
        productDietaryTagRepository.flush();
        var tagsByCode = dietaryTagRepository.findAllByCodeIn(externalProduct.dietaryTags()).stream()
                .collect(java.util.stream.Collectors.toMap(tag -> tag.getCode(), tag -> tag));
        if (tagsByCode.size() != externalProduct.dietaryTags().size()) {
            throw new IllegalArgumentException(
                    "Unknown dietary tag for product: " + externalProduct.externalId()
            );
        }
        productDietaryTagRepository.saveAll(externalProduct.dietaryTags().stream()
                .map(code -> new ProductDietaryTagEntity(product, tagsByCode.get(code)))
                .toList());
    }

    private void importAllergens(ProductEntity product, ExternalProduct externalProduct) {
        productAllergenRepository.deleteAllByProductId(product.getId());
        productAllergenRepository.flush();
        var allergenCodes = externalProduct.allergens().stream()
                .map(allergen -> allergen.code())
                .collect(java.util.stream.Collectors.toSet());
        var allergensByCode = allergenRepository.findAllByCodeIn(allergenCodes).stream()
                .collect(java.util.stream.Collectors.toMap(allergen -> allergen.getCode(), allergen -> allergen));
        if (allergensByCode.size() != allergenCodes.size()) {
            throw new IllegalArgumentException(
                    "Unknown allergen for product: " + externalProduct.externalId()
            );
        }
        productAllergenRepository.saveAll(externalProduct.allergens().stream()
                .map(allergen -> new ProductAllergenEntity(
                        product,
                        allergensByCode.get(allergen.code()),
                        allergen.presenceType()
                ))
                .toList());
    }

    private void importPriceHistory(ProductEntity product, ExternalProduct externalProduct) {
        externalProduct.priceHistory().stream()
                .filter(entry -> !priceHistoryRepository.existsByProductIdAndRecordedAt(
                        product.getId(),
                        entry.recordedAt()
                ))
                .map(entry -> new ProductPriceHistoryEntity(
                        product,
                        entry.price(),
                        entry.unitPrice(),
                        entry.recordedAt()
                ))
                .forEach(priceHistoryRepository::save);
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
