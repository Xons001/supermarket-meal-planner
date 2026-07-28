package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus;
import com.sean.supermarketmealplanner.mealplan.domain.VarietyPreference;
import com.sean.supermarketmealplanner.mealplan.domain.WarningSeverity;
import com.sean.supermarketmealplanner.mealtemplate.application.NutritionBreakdown;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record GeneratedMealPlanResult(
        boolean persisted,
        UUID mealPlanId,
        String generationToken,
        String name,
        String supermarketCode,
        String supermarketName,
        LocalDate startDate,
        int numberOfDays,
        int mealsPerDay,
        int servings,
        long seed,
        GenerationStrategy strategy,
        MealPlanStatus status,
        GenerationCriteria criteria,
        List<DayResult> days,
        NutritionBreakdown weeklyNutrition,
        BigDecimal totalConsumedCost,
        BigDecimal weeklyBudget,
        BigDecimal budgetDifference,
        boolean budgetExceeded,
        BigDecimal budgetDeviationPercentage,
        BigDecimal overallScore,
        ScoreBreakdown scoreBreakdown,
        VarietyMetrics varietyMetrics,
        boolean calculationComplete,
        List<PlanWarning> warnings,
        List<String> constraintsApplied,
        List<String> constraintsNotMet,
        Map<String, Integer> rejectedCandidateStatistics,
        GenerationMetadata generationMetadata,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record GenerationCriteria(
            BigDecimal dailyCaloriesTarget,
            BigDecimal dailyProteinTarget,
            Set<String> allowedMealTypes,
            Set<String> requiredDietaryTags,
            Set<String> excludedAllergens,
            Set<UUID> excludedTemplateIds,
            Set<UUID> excludedProductIds,
            Integer maximumPreparationMinutes,
            int maximumTemplateRepetitions,
            VarietyPreference varietyPreference,
            boolean allowIncompleteCalculations
    ) {
    }

    public record DayResult(
            int dayIndex,
            LocalDate date,
            List<PlannedMealResult> meals,
            NutritionBreakdown totalNutrition,
            BigDecimal totalConsumedCost,
            BigDecimal calorieTarget,
            BigDecimal proteinTarget,
            BigDecimal calorieDeviation,
            BigDecimal calorieDeviationPercentage,
            BigDecimal proteinDeviation,
            BigDecimal dailyScore,
            List<PlanWarning> warnings
    ) {
    }

    public record PlannedMealResult(
            int position,
            String mealType,
            UUID templateId,
            String templateName,
            int servings,
            int preparationMinutes,
            List<IngredientSummary> ingredients,
            NutritionBreakdown nutrition,
            BigDecimal consumedCost,
            BigDecimal score,
            boolean calculationComplete,
            List<String> warnings
    ) {
    }

    public record IngredientSummary(
            UUID productId,
            String productName,
            String brand,
            UUID categoryId,
            String categoryName,
            BigDecimal quantity,
            String quantityUnit,
            String measurementType,
            BigDecimal packageQuantity,
            String packageUnit,
            BigDecimal packagePrice,
            BigDecimal unitPrice,
            Boolean available,
            BigDecimal consumedCost,
            boolean calculationComplete,
            List<String> warnings,
            String quantityBasis
    ) {
    }

    public record ScoreBreakdown(
            BigDecimal calorieScore,
            BigDecimal proteinScore,
            BigDecimal budgetScore,
            BigDecimal varietyScore,
            BigDecimal repetitionScore,
            BigDecimal completenessScore,
            BigDecimal preparationScore,
            BigDecimal totalScore
    ) {
    }

    public record VarietyMetrics(
            int uniqueTemplates,
            int repeatedTemplates,
            int maximumObservedRepetition,
            BigDecimal varietyScore
    ) {
    }

    public record PlanWarning(
            String code,
            String message,
            WarningSeverity severity,
            Integer dayIndex
    ) {
    }

    public record GenerationMetadata(
            GenerationStrategy strategy,
            long seed,
            long durationMilliseconds,
            int candidatesEvaluated,
            int completePlansEvaluated,
            OffsetDateTime generatedAt,
            String algorithmVersion,
            int beamWidth,
            int candidatesPerPosition
    ) {
    }
}
