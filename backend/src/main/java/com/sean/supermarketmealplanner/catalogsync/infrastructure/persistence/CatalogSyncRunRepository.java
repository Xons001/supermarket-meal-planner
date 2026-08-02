package com.sean.supermarketmealplanner.catalogsync.infrastructure.persistence;

import com.sean.supermarketmealplanner.catalogsync.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface CatalogSyncRunRepository extends JpaRepository<CatalogSyncRunEntity, UUID> {
    @EntityGraph(attributePaths={"supermarket","retryOf"}) Optional<CatalogSyncRunEntity> findFirstByOrderByRequestedAtDesc();
    boolean existsBySupermarketIdAndStatusIn(UUID supermarketId, Collection<CatalogSyncStatus> statuses);
    @Query("select r from CatalogSyncRunEntity r join fetch r.supermarket s where " +
           "(:supermarketCode is null or s.code=:supermarketCode) and (:status is null or r.status=:status) " +
           "and (:syncType is null or r.syncType=:syncType)")
    Page<CatalogSyncRunEntity> search(com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode supermarketCode,
                                     CatalogSyncStatus status, CatalogSyncType syncType, Pageable pageable);
    @Override @EntityGraph(attributePaths={"supermarket","retryOf"}) Optional<CatalogSyncRunEntity> findById(UUID id);
}
