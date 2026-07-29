package com.sean.supermarketmealplanner.dashboard.application;

import com.sean.supermarketmealplanner.activity.application.ActivityResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        Metrics metrics,
        PlanCard latestPlan,
        ShoppingListCard selectedShoppingList,
        List<ActivityResponse> recentActivity
) {
    public record Metrics(long activePlans, long favoritePlans, long shoppingLists,
            long currentShoppingLists, long outdatedShoppingLists,
            BigDecimal averagePurchaseCost, BigDecimal averageWasteCost) {}
    public record PlanCard(UUID id, String name, LocalDate startDate, String strategy,
            BigDecimal overallScore, BigDecimal estimatedPurchaseCost, BigDecimal estimatedWasteCost,
            boolean favorite, OffsetDateTime updatedAt) {}
    public record ShoppingListCard(UUID id, UUID mealPlanId, String mealPlanName,
            BigDecimal totalPurchaseCost, BigDecimal totalWasteCost, String freshness,
            OffsetDateTime generatedAt) {}
}
