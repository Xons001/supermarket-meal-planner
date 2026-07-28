package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findBySupermarketIdAndExternalId(UUID supermarketId, String externalId);

    List<CategoryEntity> findAllBySupermarketId(UUID supermarketId);
}
