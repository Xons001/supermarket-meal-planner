package com.sean.supermarketmealplanner.nutrition.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.sean.supermarketmealplanner.nutrition.domain.NutritionEnums.*;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="nutrition_enrichment_runs")
public class NutritionEnrichmentRunEntity {
    @Id private UUID id;
    @Column(nullable=false,length=50) private String provider;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private EnrichmentStatus status;
    @Enumerated(EnumType.STRING) @Column(name="triggered_by",nullable=false,length=30) private TriggeredBy triggeredBy;
    @Column(name="requested_by") private UUID requestedBy;
    @Column(name="started_at") private OffsetDateTime startedAt;
    @Column(name="finished_at") private OffsetDateTime finishedAt;
    @Column(name="products_scanned",nullable=false) private int productsScanned;
    @Column(name="barcode_matches",nullable=false) private int barcodeMatches;
    @Column(name="name_matches",nullable=false) private int nameMatches;
    @Column(name="auto_accepted",nullable=false) private int autoAccepted;
    @Column(name="pending_review",nullable=false) private int pendingReview;
    @Column(nullable=false) private int rejected;
    @Column(name="updated_products",nullable=false) private int updatedProducts;
    @Column(name="unchanged_products",nullable=false) private int unchangedProducts;
    @Column(nullable=false) private int errors;
    @Column(name="duration_ms") private Long durationMs;
    @Column(name="report_json",nullable=false,columnDefinition="jsonb") private JsonNode reportJson;
    @Column(name="airflow_dag_run_id",length=250) private String airflowDagRunId;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    protected NutritionEnrichmentRunEntity(){}
    public NutritionEnrichmentRunEntity(String provider,TriggeredBy triggeredBy,UUID requestedBy,JsonNode report,OffsetDateTime now){
        id=UUID.randomUUID();this.provider=provider;this.triggeredBy=triggeredBy;this.requestedBy=requestedBy;
        status=EnrichmentStatus.PENDING;reportJson=report;createdAt=now;updatedAt=now;}
    public void accepted(String dagRunId,OffsetDateTime now){airflowDagRunId=dagRunId;updatedAt=now;}
    public void failed(JsonNode report,OffsetDateTime now){status=EnrichmentStatus.FAILED;reportJson=report;finishedAt=now;updatedAt=now;}
    public UUID getId(){return id;} public String getProvider(){return provider;} public EnrichmentStatus getStatus(){return status;}
    public TriggeredBy getTriggeredBy(){return triggeredBy;} public UUID getRequestedBy(){return requestedBy;}
    public OffsetDateTime getStartedAt(){return startedAt;} public OffsetDateTime getFinishedAt(){return finishedAt;}
    public int getProductsScanned(){return productsScanned;} public int getBarcodeMatches(){return barcodeMatches;}
    public int getNameMatches(){return nameMatches;} public int getAutoAccepted(){return autoAccepted;}
    public int getPendingReview(){return pendingReview;} public int getRejected(){return rejected;}
    public int getUpdatedProducts(){return updatedProducts;} public int getUnchangedProducts(){return unchangedProducts;}
    public int getErrors(){return errors;} public Long getDurationMs(){return durationMs;} public JsonNode getReportJson(){return reportJson;}
    public String getAirflowDagRunId(){return airflowDagRunId;} public OffsetDateTime getCreatedAt(){return createdAt;}
}
