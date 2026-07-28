package com.sean.supermarketmealplanner.catalog.application;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
import com.sean.supermarketmealplanner.nutrition.infrastructure.persistence.NutritionEntity;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(
            Optional<SupermarketCode> supermarketCode,
            int page,
            int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.asc("name"),
                Sort.Order.asc("id")
        ));
        var products = supermarketCode
                .map(code -> productRepository.findAllBySupermarketCode(code, pageable))
                .orElseGet(() -> productRepository.findAll(pageable))
                .map(this::mapProduct);
        return PageResponse.from(products);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID productId) {
        return productRepository.findById(productId)
                .map(this::mapProduct)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private ProductResponse mapProduct(ProductEntity product) {
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
                product.getCurrentPrice(),
                product.getUnitPrice(),
                product.getPackageQuantity(),
                product.getPackageUnit().name(),
                product.isAvailable(),
                product.getSource(),
                product.getLastSyncedAt(),
                mapNutrition(product.getNutrition()),
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
                nutrition.getDataSource(),
                nutrition.getVerificationStatus(),
                nutrition.getConfidenceScore(),
                nutrition.getUpdatedAt()
        );
    }
}
