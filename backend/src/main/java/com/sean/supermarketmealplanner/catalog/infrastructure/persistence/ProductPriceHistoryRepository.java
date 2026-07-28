package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPriceHistoryRepository
        extends JpaRepository<ProductPriceHistoryEntity, UUID> {

    boolean existsByProductIdAndRecordedAt(UUID productId, OffsetDateTime recordedAt);

    List<ProductPriceHistoryEntity> findAllByProductIdOrderByRecordedAtDesc(UUID productId);
}
