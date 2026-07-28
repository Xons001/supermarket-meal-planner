package com.sean.supermarketmealplanner.mealplan.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MealPlanSummaryResponse(
        UUID id,
        String name,
        String supermarketCode,
        String supermarketName,
        LocalDate startDate,
        int numberOfDays,
        int mealsPerDay,
        int servings,
        BigDecimal dailyCaloriesTarget,
        BigDecimal dailyProteinTarget,
        BigDecimal totalConsumedCost,
        BigDecimal weeklyBudget,
        BigDecimal overallScore,
        String status,
        boolean calculationComplete,
        int warningCount,
        long seed,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
