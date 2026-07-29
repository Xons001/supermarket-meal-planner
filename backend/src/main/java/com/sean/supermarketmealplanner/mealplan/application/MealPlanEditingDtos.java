package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.MealPlanChangeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class MealPlanEditingDtos {
    private MealPlanEditingDtos() {
    }

    public enum AlternativePriority {
        BEST_BALANCE,
        LOWER_PURCHASE_COST,
        LOWER_WASTE,
        MORE_VARIETY
    }

    public record AlternativeResponse(
            int rank,
            UUID mealTemplateId,
            String name,
            List<String> mainIngredients,
            BigDecimal calories,
            BigDecimal protein,
            BigDecimal consumedCost,
            BigDecimal marginalPurchaseCost,
            BigDecimal purchaseCostDelta,
            BigDecimal wasteCostDelta,
            int packageDelta,
            int uniqueProductDelta,
            BigDecimal varietyDelta,
            BigDecimal repetitionDelta,
            BigDecimal estimatedScore,
            List<String> reasons,
            List<String> warnings,
            long seed
    ) {
    }

    public record MetricsSnapshot(
            BigDecimal calories,
            BigDecimal protein,
            BigDecimal consumedCost,
            BigDecimal purchaseCost,
            BigDecimal wasteCost,
            BigDecimal wastePercentage,
            Integer packages,
            Integer uniqueProducts,
            BigDecimal varietyScore,
            BigDecimal repetitionScore,
            BigDecimal overallScore,
            BigDecimal budgetDifference,
            Boolean budgetExceeded
    ) {
    }

    public record EditPreviewResponse(
            String operation,
            UUID planId,
            UUID targetId,
            long editVersion,
            long seed,
            List<GeneratedMealPlanResult.PlannedMealResult> beforeMeals,
            List<GeneratedMealPlanResult.PlannedMealResult> afterMeals,
            MetricsSnapshot before,
            MetricsSnapshot after,
            MetricsSnapshot delta,
            List<String> reasons,
            List<String> warnings,
            String previewToken,
            OffsetDateTime expiresAt,
            long durationMilliseconds
    ) {
        public EditPreviewResponse withOperation(String value) {
            return new EditPreviewResponse(
                    value, planId, targetId, editVersion, seed, beforeMeals, afterMeals,
                    before, after, delta, reasons, warnings, previewToken, expiresAt,
                    durationMilliseconds
            );
        }
    }

    public record ReplacementPreviewRequest(
            @NotNull UUID mealTemplateId,
            @Min(0) long expectedEditVersion,
            Long seed
    ) {
    }

    public record RegenerationPreviewRequest(
            @Min(0) long expectedEditVersion,
            Long seed
    ) {
    }

    public record ConfirmEditRequest(
            @NotBlank String previewToken,
            @Min(0) long expectedEditVersion
    ) {
    }

    public record LockRequest(
            boolean locked,
            @Min(0) long expectedEditVersion
    ) {
    }

    public record UndoRequest(@Min(0) long expectedEditVersion) {
    }

    public record ChangeResponse(
            UUID id,
            long sequence,
            MealPlanChangeType type,
            long editVersion,
            long contentVersion,
            UUID dayId,
            UUID plannedMealId,
            MetricsSnapshot before,
            MetricsSnapshot after,
            MetricsSnapshot delta,
            Long seed,
            String strategy,
            String preset,
            String reason,
            boolean undone,
            OffsetDateTime createdAt
    ) {
    }
}
