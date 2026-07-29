package com.sean.supermarketmealplanner.shoppinglist.application;

import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListStatus;
import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListWarningSeverity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ShoppingListResponse(
        UUID id,
        UUID mealPlanId,
        String mealPlanName,
        SupermarketSnapshot supermarket,
        ShoppingListStatus status,
        OffsetDateTime generatedAt,
        OffsetDateTime updatedAt,
        List<CategoryGroup> groups,
        int itemCount,
        int totalPackages,
        BigDecimal totalConsumedCost,
        BigDecimal totalPurchaseCost,
        BigDecimal totalWasteCost,
        BigDecimal overallWastePercentage,
        Map<String, QuantitySummary> quantitySummary,
        BigDecimal weeklyBudget,
        BigDecimal purchaseBudgetDifference,
        boolean purchaseBudgetExceeded,
        BigDecimal purchaseBudgetDeviationPercentage,
        boolean budgetCalculationComplete,
        boolean calculationComplete,
        List<ShoppingWarning> warnings,
        boolean demoData,
        long generationDurationMilliseconds,
        long sourcePlanContentVersion,
        long currentPlanContentVersion,
        String freshness
) {
    public record SupermarketSnapshot(
            UUID id,
            String code,
            String name,
            String currencyCode
    ) {
    }

    public record CategoryGroup(
            UUID categoryId,
            String categoryName,
            List<Item> items,
            BigDecimal subtotalPurchaseCost,
            BigDecimal subtotalConsumedCost,
            BigDecimal subtotalWasteCost,
            boolean calculationComplete
    ) {
    }

    public record Item(
            UUID id,
            UUID productId,
            String productName,
            String brand,
            UUID categoryId,
            String categoryName,
            String measurementType,
            BigDecimal requiredQuantity,
            String requiredUnit,
            BigDecimal packageQuantity,
            String packageUnit,
            BigDecimal packagePrice,
            BigDecimal unitPrice,
            Integer packagesRequired,
            BigDecimal purchasedQuantity,
            BigDecimal leftoverQuantity,
            BigDecimal consumedCost,
            BigDecimal purchaseCost,
            BigDecimal wasteCost,
            BigDecimal leftoverPercentage,
            Boolean available,
            boolean calculationComplete,
            int sortOrder,
            List<String> warnings
    ) {
    }

    public record QuantitySummary(
            BigDecimal required,
            BigDecimal purchased,
            BigDecimal leftover,
            String unit,
            boolean calculationComplete
    ) {
    }

    public record ShoppingWarning(
            String code,
            String message,
            ShoppingListWarningSeverity severity,
            UUID itemId
    ) {
    }
}
