package com.sean.supermarketmealplanner.shoppinglist.application;

import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShoppingListSummaryResponse(
        UUID id,
        UUID mealPlanId,
        String mealPlanName,
        String supermarketCode,
        String supermarketName,
        ShoppingListStatus status,
        OffsetDateTime generatedAt,
        int itemCount,
        int totalPackages,
        BigDecimal totalConsumedCost,
        BigDecimal totalPurchaseCost,
        BigDecimal totalWasteCost,
        BigDecimal overallWastePercentage,
        BigDecimal weeklyBudget,
        boolean purchaseBudgetExceeded,
        boolean budgetCalculationComplete,
        boolean calculationComplete,
        int warningCount,
        boolean demoData
) {
}
