package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealplan.domain.OptimizationPreset;
import com.sean.supermarketmealplanner.mealplan.domain.VarietyPreference;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record GenerateMealPlanCommand(
        @NotBlank @Size(max = 50) String supermarketCode,
        @NotBlank @Size(max = 180) String name,
        @NotNull LocalDate startDate,
        @Min(1) @Max(14) int numberOfDays,
        @Min(1) @Max(6) int mealsPerDay,
        @Min(1) int servings,
        @NotNull @DecimalMin("0.1") BigDecimal dailyCaloriesTarget,
        @NotNull @DecimalMin("0") BigDecimal dailyProteinTarget,
        @DecimalMin("0.01") BigDecimal weeklyBudget,
        Set<MealType> allowedMealTypes,
        Set<String> requiredDietaryTags,
        Set<String> excludedAllergens,
        Set<UUID> excludedTemplateIds,
        Set<UUID> excludedProductIds,
        @Min(0) Integer maximumPreparationMinutes,
        @Min(1) Integer maximumTemplateRepetitions,
        VarietyPreference varietyPreference,
        boolean allowIncompleteCalculations,
        @Schema(
                description = "Generation mode; defaults to purchase-aware scoring",
                example = "PURCHASE_AWARE_SCORING"
        ) GenerationStrategy strategy,
        @Schema(
                description = "Purchase-aware priority preset; ignored and normalized to null "
                        + "when strategy is SCORING",
                example = "BALANCED"
        ) OptimizationPreset optimizationPreset,
        Long deterministicSeed,
        @Size(max = 64) String generationToken,
        boolean persist
) {
    public GenerateMealPlanCommand {
        allowedMealTypes = allowedMealTypes == null || allowedMealTypes.isEmpty()
                ? Set.copyOf(EnumSet.allOf(MealType.class))
                : Set.copyOf(allowedMealTypes);
        requiredDietaryTags = immutableUppercase(requiredDietaryTags);
        excludedAllergens = immutableUppercase(excludedAllergens);
        excludedTemplateIds = excludedTemplateIds == null ? Set.of() : Set.copyOf(excludedTemplateIds);
        excludedProductIds = excludedProductIds == null ? Set.of() : Set.copyOf(excludedProductIds);
        varietyPreference = varietyPreference == null ? VarietyPreference.MEDIUM : varietyPreference;
        strategy = strategy == null ? GenerationStrategy.PURCHASE_AWARE_SCORING : strategy;
        optimizationPreset = strategy == GenerationStrategy.SCORING
                ? null
                : optimizationPreset == null ? OptimizationPreset.BALANCED : optimizationPreset;
    }

    public GenerateMealPlanCommand(
            String supermarketCode,
            String name,
            LocalDate startDate,
            int numberOfDays,
            int mealsPerDay,
            int servings,
            BigDecimal dailyCaloriesTarget,
            BigDecimal dailyProteinTarget,
            BigDecimal weeklyBudget,
            Set<MealType> allowedMealTypes,
            Set<String> requiredDietaryTags,
            Set<String> excludedAllergens,
            Set<UUID> excludedTemplateIds,
            Set<UUID> excludedProductIds,
            Integer maximumPreparationMinutes,
            Integer maximumTemplateRepetitions,
            VarietyPreference varietyPreference,
            boolean allowIncompleteCalculations,
            Long deterministicSeed,
            String generationToken,
            boolean persist
    ) {
        this(
                supermarketCode,
                name,
                startDate,
                numberOfDays,
                mealsPerDay,
                servings,
                dailyCaloriesTarget,
                dailyProteinTarget,
                weeklyBudget,
                allowedMealTypes,
                requiredDietaryTags,
                excludedAllergens,
                excludedTemplateIds,
                excludedProductIds,
                maximumPreparationMinutes,
                maximumTemplateRepetitions,
                varietyPreference,
                allowIncompleteCalculations,
                GenerationStrategy.SCORING,
                null,
                deterministicSeed,
                generationToken,
                persist
        );
    }

    public int effectiveMaximumTemplateRepetitions() {
        return maximumTemplateRepetitions == null
                ? varietyPreference.defaultMaximumRepetitions()
                : maximumTemplateRepetitions;
    }

    public GenerateMealPlanCommand withSeedAndPersistence(
            long seed,
            String token,
            boolean shouldPersist
    ) {
        return new GenerateMealPlanCommand(
                supermarketCode,
                name,
                startDate,
                numberOfDays,
                mealsPerDay,
                servings,
                dailyCaloriesTarget,
                dailyProteinTarget,
                weeklyBudget,
                allowedMealTypes,
                requiredDietaryTags,
                excludedAllergens,
                excludedTemplateIds,
                excludedProductIds,
                maximumPreparationMinutes,
                maximumTemplateRepetitions,
                varietyPreference,
                allowIncompleteCalculations,
                strategy,
                optimizationPreset,
                seed,
                token,
                shouldPersist
        );
    }

    private static Set<String> immutableUppercase(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .map(value -> value.trim().toUpperCase(java.util.Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
