package com.sean.supermarketmealplanner.nutrition.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.nutrition.domain.NutritionEnums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="nutrition_match_candidates")
public class NutritionMatchCandidateEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="run_id") private NutritionEnrichmentRunEntity run;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id",nullable=false) private ProductEntity product;
    @Column(nullable=false,length=50) private String provider;
    @Column(name="external_reference",nullable=false,length=500) private String externalReference;
    @Column(name="external_barcode",length=64) private String externalBarcode;
    @Column(name="external_name",nullable=false,length=300) private String externalName;
    @Column(name="normalized_name",nullable=false,length=300) private String normalizedName;
    @Column(length=200) private String brand;
    @Column(name="candidate_payload_json",nullable=false,columnDefinition="jsonb") private JsonNode candidatePayload;
    @Enumerated(EnumType.STRING) @Column(name="match_method",nullable=false,length=30) private MatchMethod matchMethod;
    @Column(name="confidence_score",nullable=false,precision=5,scale=2) private BigDecimal confidenceScore;
    @Column(name="score_breakdown_json",nullable=false,columnDefinition="jsonb") private JsonNode scoreBreakdown;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private CandidateStatus status;
    @Column(name="rejection_reason",length=1000) private String rejectionReason;
    @Column(name="source_hash",nullable=false,length=64) private String sourceHash;
    @Column(name="source_updated_at") private OffsetDateTime sourceUpdatedAt;
    @Column(name="expires_at",nullable=false) private OffsetDateTime expiresAt;
    @Column(name="reviewed_at") private OffsetDateTime reviewedAt;
    @Column(name="reviewed_by") private UUID reviewedBy;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Version @Column(name="row_version",nullable=false) private long rowVersion;
    protected NutritionMatchCandidateEntity(){}
    public NutritionMatchCandidateEntity(NutritionEnrichmentRunEntity run,ProductEntity product,String provider,
            String reference,String barcode,String name,String normalized,String brand,JsonNode payload,MatchMethod method,
            BigDecimal confidence,JsonNode breakdown,CandidateStatus status,String hash,OffsetDateTime sourceUpdated,
            OffsetDateTime expires,OffsetDateTime now){id=UUID.randomUUID();this.run=run;this.product=product;this.provider=provider;
        externalReference=reference;externalBarcode=barcode;externalName=name;normalizedName=normalized;this.brand=brand;
        candidatePayload=payload;matchMethod=method;confidenceScore=confidence;scoreBreakdown=breakdown;this.status=status;
        sourceHash=hash;sourceUpdatedAt=sourceUpdated;expiresAt=expires;createdAt=now;}
    public void accept(UUID user,OffsetDateTime now){ensurePending();status=CandidateStatus.MANUALLY_ACCEPTED;reviewedBy=user;reviewedAt=now;}
    public void reject(UUID user,String reason,OffsetDateTime now){ensurePending();status=CandidateStatus.REJECTED;reviewedBy=user;reviewedAt=now;rejectionReason=reason;}
    private void ensurePending(){if(status!=CandidateStatus.PENDING)throw new IllegalStateException("candidate reviewed");}
    public UUID getId(){return id;} public NutritionEnrichmentRunEntity getRun(){return run;} public ProductEntity getProduct(){return product;}
    public String getProvider(){return provider;} public String getExternalReference(){return externalReference;}
    public String getExternalBarcode(){return externalBarcode;} public String getExternalName(){return externalName;}
    public String getNormalizedName(){return normalizedName;} public String getBrand(){return brand;} public JsonNode getCandidatePayload(){return candidatePayload;}
    public MatchMethod getMatchMethod(){return matchMethod;} public BigDecimal getConfidenceScore(){return confidenceScore;}
    public JsonNode getScoreBreakdown(){return scoreBreakdown;} public CandidateStatus getStatus(){return status;}
    public String getRejectionReason(){return rejectionReason;} public String getSourceHash(){return sourceHash;}
    public OffsetDateTime getSourceUpdatedAt(){return sourceUpdatedAt;} public OffsetDateTime getExpiresAt(){return expiresAt;}
    public OffsetDateTime getReviewedAt(){return reviewedAt;} public UUID getReviewedBy(){return reviewedBy;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public long getRowVersion(){return rowVersion;}
}
