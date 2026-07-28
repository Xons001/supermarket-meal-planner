package com.sean.supermarketmealplanner.catalog.infrastructure.bootstrap;

import com.sean.supermarketmealplanner.catalog.application.CatalogImportService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@ConditionalOnProperty(name = "app.catalog.seed-enabled", havingValue = "true", matchIfMissing = true)
public class CatalogSeedRunner implements ApplicationRunner {

    private final CatalogImportService catalogImportService;

    public CatalogSeedRunner(CatalogImportService catalogImportService) {
        this.catalogImportService = catalogImportService;
    }

    @Override
    public void run(ApplicationArguments args) {
        catalogImportService.importCatalogs();
    }
}
