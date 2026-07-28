package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductDietaryTagRepository
        extends JpaRepository<ProductDietaryTagEntity, ProductDietaryTagId> {

    void deleteAllByProductId(UUID productId);

    @EntityGraph(attributePaths = "dietaryTag")
    List<ProductDietaryTagEntity> findAllByProductIdIn(Collection<UUID> productIds);
}
