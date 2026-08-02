package com.sean.supermarketmealplanner.catalogsync.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogSyncErrorRepository extends JpaRepository<CatalogSyncErrorEntity, UUID> {
    Page<CatalogSyncErrorEntity> findBySyncRunIdOrderByCreatedAtAscIdAsc(UUID runId, Pageable pageable);
}
