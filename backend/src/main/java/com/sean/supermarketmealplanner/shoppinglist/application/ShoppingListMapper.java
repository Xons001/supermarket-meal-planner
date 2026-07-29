package com.sean.supermarketmealplanner.shoppinglist.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence.ShoppingListEntity;
import com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence.ShoppingListItemEntity;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ShoppingListMapper {

    private static final MathContext MATH = MathContext.DECIMAL128;
    private final ObjectMapper objectMapper;

    public ShoppingListMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ShoppingListResponse toResponse(ShoppingListEntity entity) {
        var itemResponses = entity.getItems().stream().map(this::toItem).toList();
        var grouped = new LinkedHashMap<CategoryKey, List<ShoppingListResponse.Item>>();
        itemResponses.stream()
                .sorted(Comparator
                        .comparing(
                                ShoppingListResponse.Item::categoryName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                        .thenComparingInt(ShoppingListResponse.Item::sortOrder)
                        .thenComparing(ShoppingListResponse.Item::productName))
                .forEach(item -> grouped.computeIfAbsent(
                        new CategoryKey(item.categoryId(), item.categoryName()),
                        ignored -> new ArrayList<>()
                ).add(item));
        var groups = grouped.entrySet().stream().map(entry -> new ShoppingListResponse.CategoryGroup(
                entry.getKey().id(),
                entry.getKey().name() == null ? "Sin categoría" : entry.getKey().name(),
                List.copyOf(entry.getValue()),
                sum(entry.getValue(), ShoppingListResponse.Item::purchaseCost),
                sum(entry.getValue(), ShoppingListResponse.Item::consumedCost),
                sum(entry.getValue(), ShoppingListResponse.Item::wasteCost),
                entry.getValue().stream().allMatch(ShoppingListResponse.Item::calculationComplete)
        )).toList();
        var warnings = entity.getWarnings().stream().map(warning ->
                new ShoppingListResponse.ShoppingWarning(
                        warning.getWarningCode(),
                        warning.getMessage(),
                        warning.getSeverity(),
                        warning.getItem() == null ? null : warning.getItem().getId()
                )
        ).toList();
        var supermarket = entity.getSupermarket();
        return new ShoppingListResponse(
                entity.getId(),
                entity.getMealPlan().getId(),
                entity.getMealPlan().getName(),
                new ShoppingListResponse.SupermarketSnapshot(
                        supermarket.getId(),
                        supermarket.getCode().name(),
                        supermarket.getName(),
                        supermarket.getCurrencyCode()
                ),
                entity.getStatus(),
                entity.getGeneratedAt(),
                entity.getUpdatedAt(),
                groups,
                itemResponses.size(),
                entity.getTotalPackages(),
                entity.getTotalConsumedCost(),
                entity.getTotalPurchaseCost(),
                entity.getTotalWasteCost(),
                entity.getOverallWastePercentage(),
                quantitySummary(entity.getQuantitySummaryJson()),
                entity.getWeeklyBudget(),
                entity.getPurchaseBudgetDifference(),
                entity.isPurchaseBudgetExceeded(),
                budgetDeviation(entity),
                entity.isBudgetCalculationComplete(),
                entity.isCalculationComplete(),
                warnings,
                entity.isDemoData(),
                entity.getGenerationDurationMilliseconds(),
                entity.getSourcePlanContentVersion(),
                entity.getMealPlan().getContentVersion(),
                entity.isCurrentForPlan() ? "CURRENT" : "OUTDATED"
        );
    }

    public ShoppingListSummaryResponse toSummary(ShoppingListEntity entity) {
        return new ShoppingListSummaryResponse(
                entity.getId(),
                entity.getMealPlan().getId(),
                entity.getMealPlan().getName(),
                entity.getSupermarket().getCode().name(),
                entity.getSupermarket().getName(),
                entity.getStatus(),
                entity.getGeneratedAt(),
                entity.getItems().size(),
                entity.getTotalPackages(),
                entity.getTotalConsumedCost(),
                entity.getTotalPurchaseCost(),
                entity.getTotalWasteCost(),
                entity.getOverallWastePercentage(),
                entity.getWeeklyBudget(),
                entity.isPurchaseBudgetExceeded(),
                entity.isBudgetCalculationComplete(),
                entity.isCalculationComplete(),
                entity.getWarnings().size(),
                entity.isDemoData(),
                entity.getSourcePlanContentVersion(),
                entity.getMealPlan().getContentVersion(),
                entity.isCurrentForPlan() ? "CURRENT" : "OUTDATED"
        );
    }

    private ShoppingListResponse.Item toItem(ShoppingListItemEntity item) {
        return new ShoppingListResponse.Item(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getBrand(),
                item.getCategoryId(),
                item.getCategoryName(),
                item.getMeasurementType(),
                item.getRequiredQuantity(),
                item.getRequiredUnit(),
                item.getPackageQuantity(),
                item.getPackageUnit(),
                item.getPackagePrice(),
                item.getUnitPrice(),
                item.getPackagesRequired(),
                item.getPurchasedQuantity(),
                item.getLeftoverQuantity(),
                item.getConsumedCost(),
                item.getPurchaseCost(),
                item.getWasteCost(),
                item.getLeftoverPercentage(),
                item.getAvailable(),
                item.isCalculationComplete(),
                item.getSortOrder(),
                stringList(item.getWarningsJson())
        );
    }

    private BigDecimal sum(
            List<ShoppingListResponse.Item> items,
            java.util.function.Function<ShoppingListResponse.Item, BigDecimal> getter
    ) {
        return items.stream().map(getter).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal budgetDeviation(ShoppingListEntity entity) {
        if (entity.getWeeklyBudget() == null || entity.getWeeklyBudget().signum() == 0) {
            return null;
        }
        return entity.getTotalPurchaseCost().subtract(entity.getWeeklyBudget()).abs()
                .multiply(new BigDecimal("100"), MATH)
                .divide(entity.getWeeklyBudget(), MATH)
                .setScale(1, RoundingMode.HALF_UP);
    }

    private Map<String, ShoppingListResponse.QuantitySummary> quantitySummary(String json) {
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, ShoppingListResponse.QuantitySummary>>() { }
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid persisted quantity summary", exception);
        }
    }

    private List<String> stringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid persisted shopping item warnings", exception);
        }
    }

    private record CategoryKey(java.util.UUID id, String name) {
    }
}
