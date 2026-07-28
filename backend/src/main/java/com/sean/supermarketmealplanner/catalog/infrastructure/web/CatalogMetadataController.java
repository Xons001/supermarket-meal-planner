package com.sean.supermarketmealplanner.catalog.infrastructure.web;

import com.sean.supermarketmealplanner.catalog.application.CatalogMetadataQueryService;
import com.sean.supermarketmealplanner.catalog.application.CatalogOptionResponse;
import com.sean.supermarketmealplanner.catalog.application.CategoryResponse;
import com.sean.supermarketmealplanner.catalog.application.ProductSearchRequestParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Catalog metadata")
public class CatalogMetadataController {

    private final CatalogMetadataQueryService queryService;
    private final ProductSearchRequestParser searchParser;

    public CatalogMetadataController(
            CatalogMetadataQueryService queryService,
            ProductSearchRequestParser searchParser
    ) {
        this.queryService = queryService;
        this.searchParser = searchParser;
    }

    @GetMapping("/api/v1/categories")
    @Operation(summary = "List active catalog categories")
    public List<CategoryResponse> findCategories(
            @RequestParam(required = false) String supermarketCode
    ) {
        return queryService.findCategories(searchParser.parseOptionalSupermarket(supermarketCode));
    }

    @GetMapping("/api/v1/dietary-tags")
    @Operation(summary = "List supported dietary tags")
    public List<CatalogOptionResponse> findDietaryTags() {
        return queryService.findDietaryTags();
    }

    @GetMapping("/api/v1/allergens")
    @Operation(summary = "List supported allergens")
    public List<CatalogOptionResponse> findAllergens() {
        return queryService.findAllergens();
    }
}
