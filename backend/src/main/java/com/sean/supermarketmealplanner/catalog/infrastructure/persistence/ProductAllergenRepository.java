package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAllergenRepository
        extends JpaRepository<ProductAllergenEntity, ProductAllergenId> {

    void deleteAllByProductId(UUID productId);

    @EntityGraph(attributePaths = "allergen")
    List<ProductAllergenEntity> findAllByProductIdIn(Collection<UUID> productIds);
}
