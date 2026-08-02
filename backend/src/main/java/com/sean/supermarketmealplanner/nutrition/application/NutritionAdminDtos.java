package com.sean.supermarketmealplanner.nutrition.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.sean.supermarketmealplanner.nutrition.domain.NutritionEnums.*;
import com.sean.supermarketmealplanner.nutrition.infrastructure.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public final class NutritionAdminDtos {
    private NutritionAdminDtos(){}
    public record RunRequest(String provider){}
    public record Accepted(UUID runId,String dagRunId,EnrichmentStatus status,OffsetDateTime createdAt){}
    public record Run(UUID id,String provider,EnrichmentStatus status,TriggeredBy triggeredBy,OffsetDateTime startedAt,
        OffsetDateTime finishedAt,int productsScanned,int barcodeMatches,int nameMatches,int autoAccepted,int pendingReview,
        int rejected,int updatedProducts,int unchangedProducts,int errors,Long durationMs,JsonNode report,String airflowDagRunId,
        OffsetDateTime createdAt){public static Run from(NutritionEnrichmentRunEntity e){return new Run(e.getId(),e.getProvider(),e.getStatus(),
            e.getTriggeredBy(),e.getStartedAt(),e.getFinishedAt(),e.getProductsScanned(),e.getBarcodeMatches(),e.getNameMatches(),
            e.getAutoAccepted(),e.getPendingReview(),e.getRejected(),e.getUpdatedProducts(),e.getUnchangedProducts(),e.getErrors(),
            e.getDurationMs(),e.getReportJson(),e.getAirflowDagRunId(),e.getCreatedAt());}}
    public record Candidate(UUID id,UUID productId,String productName,String currentSource,String provider,String externalReference,
        String externalBarcode,String externalName,String brand,MatchMethod matchMethod,BigDecimal confidenceScore,
        JsonNode scoreBreakdown,CandidateStatus status,String rejectionReason,JsonNode externalNutrition,
        NutritionSnapshot currentNutrition,OffsetDateTime expiresAt,OffsetDateTime reviewedAt,long version){
        public static Candidate from(NutritionMatchCandidateEntity e){return new Candidate(e.getId(),e.getProduct().getId(),e.getProduct().getName(),
            e.getProduct().getNutrition()==null?null:e.getProduct().getNutrition().getDataSource(),e.getProvider(),e.getExternalReference(),
            e.getExternalBarcode(),e.getExternalName(),e.getBrand(),e.getMatchMethod(),e.getConfidenceScore(),e.getScoreBreakdown(),e.getStatus(),
            e.getRejectionReason(),e.getCandidatePayload(),NutritionSnapshot.from(e.getProduct().getNutrition()),e.getExpiresAt(),e.getReviewedAt(),e.getRowVersion());}}
    public record NutritionSnapshot(String basis,BigDecimal calories,BigDecimal protein,BigDecimal carbohydrates,BigDecimal fat,
        BigDecimal fiber,BigDecimal sugars,BigDecimal salt,BigDecimal saturatedFat,String dataSource,String verificationStatus,
        BigDecimal confidenceScore,String completeness,String sourceReference,OffsetDateTime updatedAt){
        public static NutritionSnapshot from(NutritionEntity e){return e==null?null:new NutritionSnapshot(e.getNutritionBasis(),e.getCaloriesPer100g(),
            e.getProteinPer100g(),e.getCarbohydratesPer100g(),e.getFatPer100g(),e.getFiberPer100g(),e.getSugarPer100g(),e.getSaltPer100g(),
            e.getSaturatedFatPer100g(),e.getDataSource(),e.getVerificationStatus(),e.getConfidenceScore(),e.getCompleteness(),e.getSourceReference(),e.getUpdatedAt());}}
    public record NutritionInput(@NotBlank String basis,@PositiveOrZero BigDecimal calories,@PositiveOrZero BigDecimal protein,
        @PositiveOrZero BigDecimal carbohydrates,@PositiveOrZero BigDecimal fat,@PositiveOrZero BigDecimal fiber,
        @PositiveOrZero BigDecimal sugars,@PositiveOrZero BigDecimal salt,@PositiveOrZero BigDecimal saturatedFat){}
    public record ManualRequest(@Valid @NotNull NutritionInput nutrition,@NotBlank @Size(max=1000)String reason){}
    public record AcceptRequest(@Valid NutritionInput nutrition,@Size(max=1000)String reason,@NotNull Long expectedVersion){}
    public record RejectRequest(@NotBlank @Size(max=1000)String reason,@NotNull Long expectedVersion){}
    public record History(UUID id,JsonNode previousSnapshot,JsonNode newSnapshot,String changeSource,String provider,
        BigDecimal confidenceScore,OffsetDateTime changedAt,String reason){}
    public record Overview(boolean enabled,String provider,String cron,long productsWithoutNutrition,long partialProducts,
        long verifiedProducts,long pendingCandidates,Run latestRun){}
}
