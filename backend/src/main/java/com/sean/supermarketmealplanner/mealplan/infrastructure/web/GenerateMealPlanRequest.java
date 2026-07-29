package com.sean.supermarketmealplanner.mealplan.infrastructure.web;

import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealplan.domain.OptimizationPreset;
import com.sean.supermarketmealplanner.mealplan.domain.VarietyPreference;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record GenerateMealPlanRequest(
        @NotBlank @Size(max=50) String supermarketCode,
        @NotBlank @Size(max=180) String name,
        @NotNull LocalDate startDate,
        @Min(1) @Max(14) Integer numberOfDays,
        @Min(1) @Max(6) Integer mealsPerDay,
        @Min(1) Integer servings,
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
        Boolean allowIncompleteCalculations,
        GenerationStrategy strategy,
        OptimizationPreset optimizationPreset,
        Long deterministicSeed,
        @Size(max=64) String generationToken,
        Boolean persist
) {}
