package com.sean.supermarketmealplanner.mealplan.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanChangeType;
import com.sean.supermarketmealplanner.mealplan.domain.OptimizationPreset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "meal_plan_changes")
public class MealPlanChangeEntity {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlanEntity mealPlan;
    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private MealPlanChangeType changeType;
    @Column(name = "edit_version_before", nullable = false)
    private long editVersionBefore;
    @Column(name = "edit_version_after", nullable = false)
    private long editVersionAfter;
    @Column(name = "content_version_before", nullable = false)
    private long contentVersionBefore;
    @Column(name = "content_version_after", nullable = false)
    private long contentVersionAfter;
    @Column(name = "meal_plan_day_id")
    private UUID mealPlanDayId;
    @Column(name = "planned_meal_id")
    private UUID plannedMealId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_snapshot", nullable = false, columnDefinition = "jsonb")
    private JsonNode beforeSnapshot;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_snapshot", nullable = false, columnDefinition = "jsonb")
    private JsonNode afterSnapshot;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics_before", nullable = false, columnDefinition = "jsonb")
    private JsonNode metricsBefore;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics_after", nullable = false, columnDefinition = "jsonb")
    private JsonNode metricsAfter;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics_delta", nullable = false, columnDefinition = "jsonb")
    private JsonNode metricsDelta;
    @Column(name = "deterministic_seed")
    private Long deterministicSeed;
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_strategy", nullable = false, length = 30)
    private GenerationStrategy generationStrategy;
    @Enumerated(EnumType.STRING)
    @Column(name = "optimization_preset", length = 30)
    private OptimizationPreset optimizationPreset;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(name = "undone_by_change_id")
    private UUID undoneByChangeId;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected MealPlanChangeEntity() {
    }

    public MealPlanChangeEntity(
            MealPlanEntity plan,
            long sequence,
            MealPlanChangeType type,
            MealPlanEntity.VersionChange before,
            UUID dayId,
            UUID mealId,
            JsonNode beforeSnapshot,
            JsonNode afterSnapshot,
            JsonNode metricsBefore,
            JsonNode metricsAfter,
            JsonNode metricsDelta,
            Long seed,
            GenerationStrategy strategy,
            OptimizationPreset preset,
            String reason,
            OffsetDateTime now
    ) {
        this.id = UUID.randomUUID();
        this.mealPlan = plan;
        this.sequenceNumber = sequence;
        this.changeType = type;
        this.editVersionBefore = before.editVersion();
        this.editVersionAfter = plan.getEditVersion();
        this.contentVersionBefore = before.contentVersion();
        this.contentVersionAfter = plan.getContentVersion();
        this.mealPlanDayId = dayId;
        this.plannedMealId = mealId;
        this.beforeSnapshot = beforeSnapshot;
        this.afterSnapshot = afterSnapshot;
        this.metricsBefore = metricsBefore;
        this.metricsAfter = metricsAfter;
        this.metricsDelta = metricsDelta;
        this.deterministicSeed = seed;
        this.generationStrategy = strategy;
        this.optimizationPreset = preset;
        this.reason = reason;
        this.createdAt = now;
    }

    public void markUndone(UUID undoChangeId) { this.undoneByChangeId = undoChangeId; }
    public UUID getId() { return id; }
    public long getSequenceNumber() { return sequenceNumber; }
    public MealPlanChangeType getChangeType() { return changeType; }
    public long getEditVersionAfter() { return editVersionAfter; }
    public long getContentVersionAfter() { return contentVersionAfter; }
    public UUID getMealPlanDayId() { return mealPlanDayId; }
    public UUID getPlannedMealId() { return plannedMealId; }
    public JsonNode getBeforeSnapshot() { return beforeSnapshot; }
    public JsonNode getAfterSnapshot() { return afterSnapshot; }
    public JsonNode getMetricsBefore() { return metricsBefore; }
    public JsonNode getMetricsAfter() { return metricsAfter; }
    public JsonNode getMetricsDelta() { return metricsDelta; }
    public Long getDeterministicSeed() { return deterministicSeed; }
    public GenerationStrategy getGenerationStrategy() { return generationStrategy; }
    public OptimizationPreset getOptimizationPreset() { return optimizationPreset; }
    public String getReason() { return reason; }
    public UUID getUndoneByChangeId() { return undoneByChangeId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
