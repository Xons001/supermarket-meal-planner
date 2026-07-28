package com.sean.supermarketmealplanner.catalog.infrastructure.web;

import com.sean.supermarketmealplanner.catalog.application.PriceHistoryResponse;
import com.sean.supermarketmealplanner.catalog.application.ProductQueryService;
import com.sean.supermarketmealplanner.catalog.application.ProductResponse;
import com.sean.supermarketmealplanner.catalog.application.ProductSearchRequestParser;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products")
public class ProductController {

    private final ProductQueryService productQueryService;
    private final ProductSearchRequestParser searchParser;

    public ProductController(
            ProductQueryService productQueryService,
            ProductSearchRequestParser searchParser
    ) {
        this.productQueryService = productQueryService;
        this.searchParser = searchParser;
    }

    @GetMapping
    @Operation(summary = "Search the demo catalog with combinable filters")
    public PageResponse<ProductResponse> findAll(
            @RequestParam(required = false) String supermarketCode,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) BigDecimal maximumPrice,
            @RequestParam(required = false) BigDecimal maximumCalories,
            @RequestParam(required = false) BigDecimal minimumProtein,
            @RequestParam(required = false) String dietaryTags,
            @RequestParam(required = false) String excludedAllergens,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        return productQueryService.findAll(searchParser.parse(
                supermarketCode,
                categoryId,
                query,
                available,
                maximumPrice,
                maximumCalories,
                minimumProtein,
                dietaryTags,
                excludedAllergens,
                page,
                size,
                sort
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a product by its platform identifier")
    public ProductResponse findById(@PathVariable UUID id) {
        return productQueryService.findById(id);
    }

    @GetMapping("/{id}/price-history")
    @Operation(summary = "List a product's demo price history, newest first")
    public List<PriceHistoryResponse> findPriceHistory(@PathVariable UUID id) {
        return productQueryService.findPriceHistory(id);
    }
}
