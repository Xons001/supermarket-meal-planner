package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findBySupermarketIdAndExternalId(UUID supermarketId, String externalId);

    List<ProductEntity> findAllBySupermarketId(UUID supermarketId);

    @Override
    @EntityGraph(attributePaths = {"supermarket", "category", "nutrition"})
    Optional<ProductEntity> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"supermarket", "category", "nutrition"})
    Page<ProductEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"supermarket", "category", "nutrition"})
    Page<ProductEntity> findAllBySupermarketCode(SupermarketCode code, Pageable pageable);
}
