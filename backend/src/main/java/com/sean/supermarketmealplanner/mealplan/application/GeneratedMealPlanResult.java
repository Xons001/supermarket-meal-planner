package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanChangeType;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus;
import com.sean.supermarketmealplanner.mealplan.domain.MealSelectionSource;
import com.sean.supermarketmealplanner.mealplan.domain.OptimizationPreset;
import com.sean.supermarketmealplanner.mealplan.domain.ShoppingListFreshness;
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
        PurchaseMetrics purchaseMetrics,
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
        OffsetDateTime updatedAt,
        long editVersion,
        long contentVersion,
        ShoppingListFreshness shoppingListStatus,
        UUID activeShoppingListId,
        boolean canUndo,
        ChangeSummary lastChangeSummary
) {
    public GeneratedMealPlanResult(
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
            PurchaseMetrics purchaseMetrics,
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
        this(
                persisted, mealPlanId, generationToken, name, supermarketCode, supermarketName,
                startDate, numberOfDays, mealsPerDay, servings, seed, strategy, status, criteria,
                days, weeklyNutrition, totalConsumedCost, purchaseMetrics, weeklyBudget,
                budgetDifference, budgetExceeded, budgetDeviationPercentage, overallScore,
                scoreBreakdown, varietyMetrics, calculationComplete, warnings, constraintsApplied,
                constraintsNotMet, rejectedCandidateStatistics, generationMetadata, createdAt,
                updatedAt, 0, 0, ShoppingListFreshness.NONE, null, false, null
        );
    }
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
            List<PlanWarning> warnings,
            UUID dayId
    ) {
        public DayResult(
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
            this(
                    dayIndex, date, meals, totalNutrition, totalConsumedCost, calorieTarget,
                    proteinTarget, calorieDeviation, calorieDeviationPercentage,
                    proteinDeviation, dailyScore, warnings, null
            );
        }
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
            List<String> warnings,
            UUID plannedMealId,
            boolean locked,
            MealSelectionSource selectionSource,
            long editVersion,
            OffsetDateTime modifiedAt,
            UUID originalMealTemplateId,
            Long partialGenerationSeed
    ) {
        public PlannedMealResult(
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
            this(
                    position, mealType, templateId, templateName, servings, preparationMinutes,
                    ingredients, nutrition, consumedCost, score, calculationComplete, warnings,
                    null, false, MealSelectionSource.GENERATED, 0, null, null, null
            );
        }
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
            BigDecimal purchaseCostScore,
            BigDecimal consumedCostScore,
            BigDecimal purchaseBudgetScore,
            BigDecimal wasteCostScore,
            BigDecimal wastePercentageScore,
            BigDecimal usefulReuseScore,
            BigDecimal uniqueProductsScore,
            BigDecimal packageCountScore,
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

    public record PurchaseMetrics(
            BigDecimal estimatedConsumedCost,
            BigDecimal estimatedPurchaseCost,
            BigDecimal estimatedWasteCost,
            BigDecimal estimatedWastePercentage,
            int estimatedPackageCount,
            int estimatedUniqueProductCount,
            int reusedProductCount,
            int economicallyUsefulReuseCount,
            BigDecimal purchaseBudgetDifference,
            boolean purchaseBudgetExceeded,
            BigDecimal purchaseBudgetDeviationPercentage,
            boolean calculationComplete,
            List<String> warnings,
            List<String> selectionReasons
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
            int candidatesPerPosition,
            OptimizationPreset optimizationPreset,
            Map<String, BigDecimal> scoreWeights
    ) {
    }

    public record ChangeSummary(
            UUID changeId,
            MealPlanChangeType type,
            String description,
            OffsetDateTime changedAt,
            long editVersion
    ) {
    }
}
