package com.sean.supermarketmealplanner.catalogsync.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.sean.supermarketmealplanner.catalogsync.domain.*;
import com.sean.supermarketmealplanner.catalogsync.infrastructure.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class CatalogSyncDtos {
    private CatalogSyncDtos() {}
    public record TriggerRequest(@NotBlank String supermarketCode, @NotNull CatalogSyncType syncType) {}
    public record Accepted(UUID syncRunId, String dagId, String dagRunId, CatalogSyncStatus status, OffsetDateTime requestedAt) {}
    public record Run(UUID id, String supermarketCode, CatalogSyncType syncType, CatalogSyncTrigger triggeredBy,
        CatalogSyncStatus status, String provider, String airflowDagId, String airflowDagRunId, UUID retryOfSyncRunId,
        int categoriesProcessed, int productsProcessed, int productsCreated, int productsUpdated,
        int productsUnavailable, int pricesChanged, int validationErrors, JsonNode result,
        OffsetDateTime requestedAt, OffsetDateTime startedAt, OffsetDateTime completedAt) {
        public static Run from(CatalogSyncRunEntity e) { return new Run(e.getId(), e.getSupermarket().getCode().name(),
            e.getSyncType(), e.getTriggeredBy(), e.getStatus(), e.getProvider(), e.getAirflowDagId(), e.getAirflowDagRunId(),
            e.getRetryOf()==null?null:e.getRetryOf().getId(), e.getCategoriesProcessed(), e.getProductsProcessed(),
            e.getProductsCreated(), e.getProductsUpdated(), e.getProductsUnavailable(), e.getPricesChanged(),
            e.getValidationErrors(), e.getResultJson(), e.getRequestedAt(), e.getStartedAt(), e.getCompletedAt()); }
    }
    public record Error(UUID id, String severity, String entityType, String externalId, String errorCode,
                        String message, OffsetDateTime createdAt) {
        public static Error from(CatalogSyncErrorEntity e) { return new Error(e.getId(),e.getSeverity(),e.getEntityType(),
                e.getExternalId(),e.getErrorCode(),e.getMessage(),e.getCreatedAt()); }
    }
    public record Overview(boolean enabled, boolean airflowHealthy, String provider, String airflowPublicUrl,
                           String fullSchedule, String priceSchedule, Run latestRun) {}
}
