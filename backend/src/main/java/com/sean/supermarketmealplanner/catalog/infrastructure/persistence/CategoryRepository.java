package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findBySupermarketIdAndExternalId(UUID supermarketId, String externalId);

    @Override
    @EntityGraph(attributePaths = "supermarket")
    Optional<CategoryEntity> findById(UUID id);

    List<CategoryEntity> findAllBySupermarketId(UUID supermarketId);

    @EntityGraph(attributePaths = "supermarket")
    List<CategoryEntity> findAllByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = "supermarket")
    List<CategoryEntity> findAllBySupermarketCodeAndActiveTrueOrderByNameAsc(
            com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode code
    );
}
