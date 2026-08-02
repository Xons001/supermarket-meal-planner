package com.sean.supermarketmealplanner.catalogsync.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.sean.supermarketmealplanner.catalogsync.domain.CatalogSyncStatus;
import com.sean.supermarketmealplanner.catalogsync.domain.CatalogSyncTrigger;
import com.sean.supermarketmealplanner.catalogsync.domain.CatalogSyncType;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "catalog_sync_runs")
public class CatalogSyncRunEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supermarket_id", nullable = false) private SupermarketEntity supermarket;
    @Enumerated(EnumType.STRING) @Column(name="sync_type", nullable=false) private CatalogSyncType syncType;
    @Enumerated(EnumType.STRING) @Column(name="triggered_by", nullable=false) private CatalogSyncTrigger triggeredBy;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private CatalogSyncStatus status;
    @Column(nullable=false, length=50) private String provider;
    @Column(name="airflow_dag_id", length=120) private String airflowDagId;
    @Column(name="airflow_dag_run_id", length=200) private String airflowDagRunId;
    @Column(name="requested_by_user_id") private UUID requestedByUserId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="retry_of_sync_run_id") private CatalogSyncRunEntity retryOf;
    @Column(name="categories_processed", nullable=false) private int categoriesProcessed;
    @Column(name="products_processed", nullable=false) private int productsProcessed;
    @Column(name="products_created", nullable=false) private int productsCreated;
    @Column(name="products_updated", nullable=false) private int productsUpdated;
    @Column(name="products_unavailable", nullable=false) private int productsUnavailable;
    @Column(name="prices_changed", nullable=false) private int pricesChanged;
    @Column(name="validation_errors", nullable=false) private int validationErrors;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="configuration_json", columnDefinition="jsonb", nullable=false) private JsonNode configurationJson;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="result_json", columnDefinition="jsonb") private JsonNode resultJson;
    @Column(name="requested_at", nullable=false) private OffsetDateTime requestedAt;
    @Column(name="started_at") private OffsetDateTime startedAt;
    @Column(name="completed_at") private OffsetDateTime completedAt;
    @Column(name="updated_at", nullable=false) private OffsetDateTime updatedAt;

    protected CatalogSyncRunEntity() {}
    public CatalogSyncRunEntity(SupermarketEntity supermarket, CatalogSyncType syncType, CatalogSyncTrigger triggeredBy,
            String provider, UUID requestedByUserId, CatalogSyncRunEntity retryOf, JsonNode configuration, OffsetDateTime now) {
        id=UUID.randomUUID(); this.supermarket=supermarket; this.syncType=syncType; this.triggeredBy=triggeredBy;
        status=CatalogSyncStatus.PENDING; this.provider=provider; this.requestedByUserId=requestedByUserId;
        this.retryOf=retryOf; configurationJson=configuration; requestedAt=now; updatedAt=now;
    }
    public void accepted(String dagId, String dagRunId, OffsetDateTime now) { airflowDagId=dagId; airflowDagRunId=dagRunId; updatedAt=now; }
    public void fail(JsonNode result, OffsetDateTime now) { status=CatalogSyncStatus.FAILED; resultJson=result; completedAt=now; updatedAt=now; }
    public UUID getId(){return id;} public SupermarketEntity getSupermarket(){return supermarket;}
    public CatalogSyncType getSyncType(){return syncType;} public CatalogSyncTrigger getTriggeredBy(){return triggeredBy;}
    public CatalogSyncStatus getStatus(){return status;} public String getProvider(){return provider;}
    public String getAirflowDagId(){return airflowDagId;} public String getAirflowDagRunId(){return airflowDagRunId;}
    public UUID getRequestedByUserId(){return requestedByUserId;} public CatalogSyncRunEntity getRetryOf(){return retryOf;}
    public int getCategoriesProcessed(){return categoriesProcessed;} public int getProductsProcessed(){return productsProcessed;}
    public int getProductsCreated(){return productsCreated;} public int getProductsUpdated(){return productsUpdated;}
    public int getProductsUnavailable(){return productsUnavailable;} public int getPricesChanged(){return pricesChanged;}
    public int getValidationErrors(){return validationErrors;} public JsonNode getConfigurationJson(){return configurationJson;}
    public JsonNode getResultJson(){return resultJson;} public OffsetDateTime getRequestedAt(){return requestedAt;}
    public OffsetDateTime getStartedAt(){return startedAt;} public OffsetDateTime getCompletedAt(){return completedAt;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
