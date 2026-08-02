package com.sean.supermarketmealplanner.catalogsync.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="catalog_sync_errors")
public class CatalogSyncErrorEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="sync_run_id", nullable=false) private CatalogSyncRunEntity syncRun;
    @Column(nullable=false, length=12) private String severity;
    @Column(name="entity_type", nullable=false, length=40) private String entityType;
    @Column(name="external_id", length=160) private String externalId;
    @Column(name="error_code", nullable=false, length=80) private String errorCode;
    @Column(nullable=false, columnDefinition="text") private String message;
    @Column(name="raw_data_hash", nullable=false, length=64) private String rawDataHash;
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
    protected CatalogSyncErrorEntity() {}
    public UUID getId(){return id;} public String getSeverity(){return severity;} public String getEntityType(){return entityType;}
    public String getExternalId(){return externalId;} public String getErrorCode(){return errorCode;}
    public String getMessage(){return message;} public OffsetDateTime getCreatedAt(){return createdAt;}
}
