package com.sean.supermarketmealplanner.catalog.infrastructure.web;

import com.sean.supermarketmealplanner.catalog.application.ProductQueryService;
import com.sean.supermarketmealplanner.catalog.application.ProductResponse;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Optional;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@Validated
@Tag(name = "Products")
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @GetMapping
    @Operation(summary = "List demo catalog products")
    public PageResponse<ProductResponse> findAll(
            @RequestParam Optional<SupermarketCode> supermarketCode,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return productQueryService.findAll(supermarketCode, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a product by its platform identifier")
    public ProductResponse findById(@PathVariable UUID id) {
        return productQueryService.findById(id);
    }
}
