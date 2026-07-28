package com.sean.supermarketmealplanner.catalog.application;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductAllergenEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductAllergenRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductDietaryTagEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductDietaryTagRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductPriceHistoryRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductSpecifications;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductDietaryTagRepository productDietaryTagRepository;
    private final ProductAllergenRepository productAllergenRepository;
    private final ProductPriceHistoryRepository priceHistoryRepository;
    private final ProductResponseMapper mapper;

    public ProductQueryService(
            ProductRepository productRepository,
            ProductDietaryTagRepository productDietaryTagRepository,
            ProductAllergenRepository productAllergenRepository,
            ProductPriceHistoryRepository priceHistoryRepository,
            ProductResponseMapper mapper
    ) {
        this.productRepository = productRepository;
        this.productDietaryTagRepository = productDietaryTagRepository;
        this.productAllergenRepository = productAllergenRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(ParsedProductSearch search) {
        var page = productRepository.findAll(
                ProductSpecifications.matching(search.criteria()),
                search.pageable()
        );
        var ids = page.getContent().stream().map(ProductEntity::getId).toList();
        var tags = groupTags(ids);
        var allergens = groupAllergens(ids);
        return PageResponse.from(page.map(product -> mapper.map(
                product,
                tags.getOrDefault(product.getId(), List.of()),
                allergens.getOrDefault(product.getId(), List.of())
        )));
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID productId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return mapper.map(
                product,
                productDietaryTagRepository.findAllByProductIdIn(List.of(productId)),
                productAllergenRepository.findAllByProductIdIn(List.of(productId))
        );
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> findPriceHistory(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        return priceHistoryRepository.findAllByProductIdOrderByRecordedAtDesc(productId).stream()
                .map(entry -> new PriceHistoryResponse(
                        entry.getId(),
                        entry.getPrice(),
                        entry.getUnitPrice(),
                        entry.getRecordedAt(),
                        true
                ))
                .toList();
    }

    private Map<UUID, List<ProductDietaryTagEntity>> groupTags(List<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productDietaryTagRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(relation -> relation.getProduct().getId()));
    }

    private Map<UUID, List<ProductAllergenEntity>> groupAllergens(List<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productAllergenRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(relation -> relation.getProduct().getId()));
    }
}
