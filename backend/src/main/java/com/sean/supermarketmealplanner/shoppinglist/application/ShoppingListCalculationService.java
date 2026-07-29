package com.sean.supermarketmealplanner.shoppinglist.application;

import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.shared.application.purchase.PurchaseMetricsCalculator;
import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListWarningSeverity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ShoppingListCalculationService {

    private final PurchaseMetricsCalculator calculator;

    public ShoppingListCalculationService(PurchaseMetricsCalculator calculator) {
        this.calculator = calculator;
    }

    public ShoppingListCalculation calculate(GeneratedMealPlanResult plan) {
        var ingredients = plan.days().stream()
                .sorted(java.util.Comparator.comparingInt(
                        GeneratedMealPlanResult.DayResult::dayIndex
                ))
                .flatMap(day -> day.meals().stream()
                        .sorted(java.util.Comparator.comparingInt(
                                GeneratedMealPlanResult.PlannedMealResult::position
                        )))
                .flatMap(meal -> meal.ingredients().stream())
                .map(this::input)
                .toList();
        final PurchaseMetricsCalculator.PurchaseCalculation purchase;
        try {
            purchase = calculator.calculate(
                    ingredients,
                    plan.weeklyBudget(),
                    PurchaseMetricsCalculator.ConflictMode.STRICT
            );
        } catch (PurchaseMetricsCalculator.PurchaseUnitConflictException exception) {
            throw new ShoppingListException(
                    exception.getMessage(),
                    "SHOPPING_LIST_UNIT_INCOMPATIBLE",
                    422,
                    exception.productId(),
                    exception.productName(),
                    exception.units(),
                    exception.measurementType()
            );
        } catch (PurchaseMetricsCalculator.InvalidPurchaseInputException exception) {
            throw new ShoppingListException(
                    exception.getMessage(),
                    exception.code(),
                    422
            );
        }

        var itemIds = new LinkedHashMap<UUID, UUID>();
        var items = new ArrayList<ShoppingListResponse.Item>();
        var sortOrder = 0;
        for (var line : purchase.lines()) {
            var itemId = UUID.randomUUID();
            itemIds.put(line.productId(), itemId);
            items.add(new ShoppingListResponse.Item(
                    itemId,
                    line.productId(),
                    line.productName(),
                    line.brand(),
                    line.categoryId(),
                    line.categoryName(),
                    line.measurementType(),
                    line.requiredQuantity(),
                    line.requiredUnit(),
                    line.packageQuantity(),
                    line.packageUnit(),
                    line.packagePrice(),
                    line.unitPrice(),
                    line.packagesRequired(),
                    line.purchasedQuantity(),
                    line.leftoverQuantity(),
                    line.consumedCost(),
                    line.purchaseCost(),
                    line.wasteCost(),
                    line.wastePercentage(),
                    line.available(),
                    line.calculationComplete(),
                    sortOrder++,
                    line.warnings()
            ));
        }
        var warnings = purchase.warnings().stream().map(warning ->
                new ShoppingListResponse.ShoppingWarning(
                        warning.code(),
                        warning.message(),
                        ShoppingListWarningSeverity.WARNING,
                        warning.productId() == null ? null : itemIds.get(warning.productId())
                )).toList();

        return new ShoppingListCalculation(
                List.copyOf(items),
                purchase.totalPackages(),
                purchase.totalConsumedCost(),
                purchase.totalPurchaseCost(),
                purchase.totalWasteCost(),
                purchase.wastePercentage(),
                quantitySummary(items),
                plan.weeklyBudget(),
                purchase.purchaseBudgetDifference(),
                purchase.purchaseBudgetExceeded(),
                purchase.purchaseBudgetDeviationPercentage(),
                purchase.calculationComplete(),
                purchase.calculationComplete(),
                warnings
        );
    }

    private PurchaseMetricsCalculator.IngredientInput input(
            GeneratedMealPlanResult.IngredientSummary ingredient
    ) {
        return new PurchaseMetricsCalculator.IngredientInput(
                ingredient.productId(),
                ingredient.productName(),
                ingredient.brand(),
                ingredient.categoryId(),
                ingredient.categoryName(),
                ingredient.quantity(),
                ingredient.quantityUnit(),
                ingredient.measurementType(),
                ingredient.packageQuantity(),
                ingredient.packageUnit(),
                ingredient.packagePrice(),
                ingredient.unitPrice(),
                ingredient.available(),
                ingredient.warnings()
        );
    }

    private Map<String, ShoppingListResponse.QuantitySummary> quantitySummary(
            List<ShoppingListResponse.Item> items
    ) {
        var values = new LinkedHashMap<String, ShoppingListResponse.QuantitySummary>();
        for (var type : List.of("WEIGHT", "VOLUME", "UNIT")) {
            var matching = items.stream().filter(item -> type.equals(item.measurementType())).toList();
            var required = quantity(matching.stream()
                    .map(ShoppingListResponse.Item::requiredQuantity)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            var purchased = quantity(matching.stream()
                    .filter(ShoppingListResponse.Item::calculationComplete)
                    .map(ShoppingListResponse.Item::purchasedQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            var leftover = quantity(matching.stream()
                    .filter(ShoppingListResponse.Item::calculationComplete)
                    .map(ShoppingListResponse.Item::leftoverQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            values.put(type, new ShoppingListResponse.QuantitySummary(
                    required,
                    purchased,
                    leftover,
                    baseUnit(type),
                    matching.stream().allMatch(ShoppingListResponse.Item::calculationComplete)
            ));
        }
        return Map.copyOf(values);
    }

    private String baseUnit(String measurementType) {
        return switch (measurementType) {
            case "WEIGHT" -> "GRAM";
            case "VOLUME" -> "MILLILITER";
            case "UNIT" -> "UNIT";
            default -> null;
        };
    }

    private BigDecimal quantity(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
    }
}
