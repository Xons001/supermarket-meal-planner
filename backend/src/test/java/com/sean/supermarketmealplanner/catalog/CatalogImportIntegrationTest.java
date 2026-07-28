package com.sean.supermarketmealplanner.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.sean.supermarketmealplanner.AbstractIntegrationTest;
import com.sean.supermarketmealplanner.catalog.application.CatalogImportService;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
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
    private SupermarketRepository supermarketRepository;

    @Test
    void controlledDataIsValidAndImportIsIdempotent() {
        var document = fileReader.read();

        assertThat(document.metadata().classification()).isEqualTo("DEMONSTRATION_DATA");
        assertThat(document.products()).hasSize(12);
        assertThat(document.products())
                .extracting(product -> product.externalId())
                .doesNotHaveDuplicates();
        assertThat(document.products())
                .allSatisfy(product -> {
                    assertThat(product.currentPrice()).isNotNegative();
                    assertThat(product.packageQuantity()).isPositive();
                    assertThat(product.nutrition()).isNotNull();
                });

        assertThat(supermarketRepository.count()).isEqualTo(4);
        assertThat(productRepository.count()).isEqualTo(12);
        assertThat(nutritionRepository.count()).isEqualTo(12);

        catalogImportService.importCatalogs();

        assertThat(productRepository.count()).isEqualTo(12);
        assertThat(nutritionRepository.count()).isEqualTo(12);
    }
}
