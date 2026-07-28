package com.sean.supermarketmealplanner.catalog.infrastructure.provider;

import com.sean.supermarketmealplanner.catalog.application.port.ExternalCategory;
import com.sean.supermarketmealplanner.catalog.application.port.ExternalProduct;
import com.sean.supermarketmealplanner.catalog.application.port.SupermarketCatalogProvider;
import com.sean.supermarketmealplanner.catalog.domain.PackageUnit;
import com.sean.supermarketmealplanner.shared.infrastructure.demodata.DemoCatalogFileReader;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LocalJsonSupermarketCatalogProvider implements SupermarketCatalogProvider {

    private final DemoCatalogFileReader fileReader;

    public LocalJsonSupermarketCatalogProvider(DemoCatalogFileReader fileReader) {
        this.fileReader = fileReader;
    }

    @Override
    public SupermarketCode supportedSupermarket() {
        return SupermarketCode.valueOf(fileReader.read().supermarketCode());
    }

    @Override
    public List<ExternalCategory> fetchCategories() {
        return fileReader.read().categories().stream()
                .map(category -> new ExternalCategory(
                        category.externalId(),
                        category.name(),
                        category.parentExternalId()
                ))
                .toList();
    }

    @Override
    public List<ExternalProduct> fetchProducts() {
        return fileReader.read().products().stream()
                .map(this::mapProduct)
                .toList();
    }

    @Override
    public Optional<ExternalProduct> fetchProduct(String externalId) {
        return fileReader.read().products().stream()
                .filter(product -> product.externalId().equals(externalId))
                .findFirst()
                .map(this::mapProduct);
    }

    private ExternalProduct mapProduct(
            com.sean.supermarketmealplanner.shared.infrastructure.demodata.DemoCatalogDocument.DemoProduct product
    ) {
        return new ExternalProduct(
                product.externalId(),
                product.barcode(),
                product.categoryExternalId(),
                product.name(),
                product.brand(),
                product.description(),
                product.packageQuantity(),
                PackageUnit.valueOf(product.packageUnit()),
                product.currentPrice(),
                product.unitPrice(),
                product.available(),
                product.source()
        );
    }
}
