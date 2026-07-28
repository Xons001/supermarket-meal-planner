package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID>,
        JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findBySupermarketIdAndExternalId(UUID supermarketId, String externalId);

    List<ProductEntity> findAllBySupermarketId(UUID supermarketId);

    @Override
    @EntityGraph(attributePaths = {"supermarket", "category", "nutrition"})
    Optional<ProductEntity> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"supermarket", "category", "nutrition"})
    Page<ProductEntity> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"supermarket", "category", "nutrition"})
    Page<ProductEntity> findAll(Specification<ProductEntity> specification, Pageable pageable);
}
