package com.sean.supermarketmealplanner.shoppinglist.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ShoppingListCalculation(
        List<ShoppingListResponse.Item> items,
        int totalPackages,
        BigDecimal totalConsumedCost,
        BigDecimal totalPurchaseCost,
        BigDecimal totalWasteCost,
        BigDecimal overallWastePercentage,
        Map<String, ShoppingListResponse.QuantitySummary> quantitySummary,
        BigDecimal weeklyBudget,
        BigDecimal purchaseBudgetDifference,
        boolean purchaseBudgetExceeded,
        BigDecimal purchaseBudgetDeviationPercentage,
        boolean budgetCalculationComplete,
        boolean calculationComplete,
        List<ShoppingListResponse.ShoppingWarning> warnings
) {
}
