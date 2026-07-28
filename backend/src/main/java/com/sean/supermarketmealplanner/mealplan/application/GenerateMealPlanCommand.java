package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.VarietyPreference;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
