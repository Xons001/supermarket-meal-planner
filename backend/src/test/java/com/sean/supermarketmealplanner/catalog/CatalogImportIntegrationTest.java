package com.sean.supermarketmealplanner.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.sean.supermarketmealplanner.AbstractIntegrationTest;
import com.sean.supermarketmealplanner.catalog.application.CatalogImportService;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductDietaryTagRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductAllergenRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductPriceHistoryRepository;
import com.sean.supermarketmealplanner.nutrition.infrastructure.persistence.NutritionRepository;
import com.sean.supermarketmealplanner.shared.infrastructure.demodata.DemoCatalogFileReader;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CatalogImportIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CatalogImportService catalogImportService;

    @Autowired
    private DemoCatalogFileReader fileReader;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NutritionRepository nutritionRepository;

    @Autowired
    private ProductDietaryTagRepository productDietaryTagRepository;

    @Autowired
    private ProductAllergenRepository productAllergenRepository;

    @Autowired
    private ProductPriceHistoryRepository priceHistoryRepository;

    @Autowired
    private SupermarketRepository supermarketRepository;

    @Test
    void controlledDataIsValidAndImportIsIdempotent() {
        var document = fileReader.read();

        assertThat(document.metadata().classification()).isEqualTo("DEMONSTRATION_DATA");
        assertThat(document.products()).hasSize(24);
        assertThat(document.products())
                .extracting(product -> product.externalId())
                .doesNotHaveDuplicates();
        assertThat(document.products())
                .allSatisfy(product -> {
                    assertThat(product.currentPrice()).isNotNegative();
                    assertThat(product.packageQuantity()).isPositive();
                    assertThat(product.priceHistory()).isNotEmpty();
                    assertThat(product.dietaryTags()).isNotEmpty();
                });
        assertThat(document.products().stream().filter(product -> product.nutrition() == null))
                .hasSize(2);

        assertThat(supermarketRepository.count()).isEqualTo(4);
        assertThat(productRepository.count()).isEqualTo(24);
        assertThat(nutritionRepository.count()).isEqualTo(22);
        assertThat(productDietaryTagRepository.count()).isGreaterThan(24);
        assertThat(productAllergenRepository.count()).isGreaterThan(10);
        assertThat(priceHistoryRepository.count()).isEqualTo(51);

        var tagCount = productDietaryTagRepository.count();
        var allergenCount = productAllergenRepository.count();
        var priceCount = priceHistoryRepository.count();

        catalogImportService.importCatalogs();

        assertThat(productRepository.count()).isEqualTo(24);
        assertThat(nutritionRepository.count()).isEqualTo(22);
        assertThat(productDietaryTagRepository.count()).isEqualTo(tagCount);
        assertThat(productAllergenRepository.count()).isEqualTo(allergenCount);
        assertThat(priceHistoryRepository.count()).isEqualTo(priceCount);
    }
}
