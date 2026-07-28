package com.sean.supermarketmealplanner.shoppinglist.application;

import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListWarningSeverity;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ShoppingListCalculationService {

    private static final MathContext MATH = MathContext.DECIMAL128;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public ShoppingListCalculation calculate(GeneratedMealPlanResult plan) {
        var aggregated = new LinkedHashMap<UUID, Aggregate>();
        plan.days().stream()
                .sorted(Comparator.comparingInt(GeneratedMealPlanResult.DayResult::dayIndex))
                .flatMap(day -> day.meals().stream()
                        .sorted(Comparator.comparingInt(
                                GeneratedMealPlanResult.PlannedMealResult::position
                        )))
                .flatMap(meal -> meal.ingredients().stream())
                .forEach(ingredient -> aggregate(aggregated, ingredient));

        var items = new ArrayList<ShoppingListResponse.Item>();
        var warnings = new ArrayList<ShoppingListResponse.ShoppingWarning>();
        var sortOrder = 0;
        for (var aggregate : aggregated.values()) {
            var calculated = calculateItem(aggregate, sortOrder++);
            items.add(calculated.item());
            warnings.addAll(calculated.warnings());
        }
        items.sort(Comparator
                .comparing(
                        ShoppingListResponse.Item::categoryName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                )
                .thenComparingInt(ShoppingListResponse.Item::sortOrder)
                .thenComparing(ShoppingListResponse.Item::productName));

        var calculatedItems = items.stream().filter(
                ShoppingListResponse.Item::calculationComplete
        ).toList();
        var consumed = money(calculatedItems.stream()
                .map(ShoppingListResponse.Item::consumedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        var purchase = money(calculatedItems.stream()
                .map(ShoppingListResponse.Item::purchaseCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        var waste = money(calculatedItems.stream()
                .map(ShoppingListResponse.Item::wasteCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        var totalPackages = calculatedItems.stream()
                .mapToInt(ShoppingListResponse.Item::packagesRequired)
                .sum();
        var complete = items.stream().allMatch(ShoppingListResponse.Item::calculationComplete);
        var overallWaste = purchase.signum() == 0
                ? BigDecimal.ZERO.setScale(1)
                : percentage(waste, purchase);
        var quantitySummary = quantitySummary(items);
        var budget = plan.weeklyBudget();
        var budgetDifference = budget == null ? null : money(budget.subtract(purchase));
        var budgetExceeded = budget != null && purchase.compareTo(budget) > 0;
        var budgetDeviation = budget == null
                ? null
                : percentage(purchase.subtract(budget).abs(), budget);
        var budgetComplete = complete;
        if (!complete) {
            warnings.add(new ShoppingListResponse.ShoppingWarning(
                    "SHOPPING_LIST_CALCULATION_PARTIAL",
                    "Purchase cost and budget comparison are partial because some products "
                            + "could not be calculated",
                    ShoppingListWarningSeverity.WARNING,
                    null
            ));
        }
        return new ShoppingListCalculation(
                List.copyOf(items),
                totalPackages,
                consumed,
                purchase,
                waste,
                overallWaste,
                quantitySummary,
                budget,
                budgetDifference,
                budgetExceeded,
                budgetDeviation,
                budgetComplete,
                complete,
                List.copyOf(warnings)
        );
    }

    private void aggregate(
            Map<UUID, Aggregate> values,
            GeneratedMealPlanResult.IngredientSummary ingredient
    ) {
        if (ingredient.productId() == null) {
            throw new ShoppingListException(
                    "A planned ingredient has no productId snapshot",
                    "PRODUCT_SNAPSHOT_INCOMPLETE",
                    422
            );
        }
        if (ingredient.quantity() == null || ingredient.quantity().signum() < 0) {
            throw new ShoppingListException(
                    "Invalid required quantity for product " + ingredient.productId(),
                    "INVALID_REQUIRED_QUANTITY",
                    422
            );
        }
        var current = values.get(ingredient.productId());
        if (current == null) {
            values.put(ingredient.productId(), new Aggregate(ingredient));
            return;
        }
        current.add(ingredient);
    }

    private CalculatedItem calculateItem(Aggregate aggregate, int sortOrder) {
        var warnings = new LinkedHashSet<String>(aggregate.warnings);
        var warningResponses = new ArrayList<ShoppingListResponse.ShoppingWarning>();
        if (Boolean.FALSE.equals(aggregate.available)) {
            warnings.add("PRODUCT_UNAVAILABLE: Product was unavailable when the plan was generated");
        }
        var packageBase = packageBaseQuantity(
                aggregate.packageQuantity,
                aggregate.packageUnit,
                aggregate.measurementType
        );
        String missingCode = null;
        if (aggregate.measurementType == null || aggregate.requiredUnit == null) {
            missingCode = "PRODUCT_SNAPSHOT_INCOMPLETE";
        } else if (aggregate.packageQuantity == null || aggregate.packageUnit == null) {
            missingCode = "PACKAGE_DATA_MISSING";
        } else if (aggregate.packagePrice == null) {
            missingCode = "PACKAGE_PRICE_MISSING";
        } else if (packageBase == null) {
            missingCode = "PACKAGE_UNIT_INCOMPATIBLE";
        } else if (aggregate.packageQuantity.signum() <= 0 || aggregate.packagePrice.signum() < 0) {
            missingCode = "PACKAGE_DATA_INVALID";
        }

        Integer packages = null;
        BigDecimal purchased = null;
        BigDecimal leftover = null;
        BigDecimal consumed = null;
        BigDecimal purchase = null;
        BigDecimal waste = null;
        BigDecimal leftoverPercentage = null;
        var complete = missingCode == null;
        if (complete) {
            packages = aggregate.requiredQuantity.divide(
                    packageBase,
                    0,
                    RoundingMode.CEILING
            ).intValueExact();
            purchased = quantity(packageBase.multiply(BigDecimal.valueOf(packages), MATH));
            leftover = quantity(purchased.subtract(aggregate.requiredQuantity, MATH));
            purchase = money(aggregate.packagePrice.multiply(BigDecimal.valueOf(packages), MATH));
            consumed = money(aggregate.packagePrice
                    .multiply(aggregate.requiredQuantity, MATH)
                    .divide(packageBase, MATH));
            waste = money(purchase.subtract(consumed, MATH));
            leftoverPercentage = purchased.signum() == 0
                    ? BigDecimal.ZERO.setScale(1)
                    : percentage(leftover, purchased);
        } else {
            warnings.add(missingCode + ": Package calculation is unavailable for this snapshot");
        }
        var itemId = UUID.randomUUID();
        for (var warning : warnings) {
            var separator = warning.indexOf(':');
            var code = separator > 0
                    ? warning.substring(0, separator).trim().replace(' ', '_').toUpperCase()
                    : "PRODUCT_WARNING";
            warningResponses.add(new ShoppingListResponse.ShoppingWarning(
                    code,
                    warning,
                    ShoppingListWarningSeverity.WARNING,
                    itemId
            ));
        }
        var item = new ShoppingListResponse.Item(
                itemId,
                aggregate.productId,
                aggregate.productName,
                aggregate.brand,
                aggregate.categoryId,
                aggregate.categoryName,
                aggregate.measurementType,
                quantity(aggregate.requiredQuantity),
                aggregate.requiredUnit,
                aggregate.packageQuantity,
                aggregate.packageUnit,
                aggregate.packagePrice,
                aggregate.unitPrice,
                packages,
                purchased,
                leftover,
                consumed,
                purchase,
                waste,
                leftoverPercentage,
                aggregate.available,
                complete,
                sortOrder,
                List.copyOf(warnings)
        );
        return new CalculatedItem(item, List.copyOf(warningResponses));
    }

    private Map<String, ShoppingListResponse.QuantitySummary> quantitySummary(
            List<ShoppingListResponse.Item> items
    ) {
        var values = new LinkedHashMap<String, ShoppingListResponse.QuantitySummary>();
        for (var type : List.of("WEIGHT", "VOLUME", "UNIT")) {
            var matching = items.stream().filter(item -> type.equals(item.measurementType())).toList();
            var required = quantity(matching.stream().map(ShoppingListResponse.Item::requiredQuantity)
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

    private BigDecimal packageBaseQuantity(
            BigDecimal packageQuantity,
            String packageUnit,
            String measurementType
    ) {
        if (packageQuantity == null || packageUnit == null || measurementType == null) {
            return null;
        }
        return switch (measurementType) {
            case "WEIGHT" -> switch (packageUnit) {
                case "G" -> packageQuantity;
                case "KG" -> packageQuantity.multiply(new BigDecimal("1000"), MATH);
                default -> null;
            };
            case "VOLUME" -> switch (packageUnit) {
                case "ML" -> packageQuantity;
                case "L" -> packageQuantity.multiply(new BigDecimal("1000"), MATH);
                default -> null;
            };
            case "UNIT" -> "UNIT".equals(packageUnit) ? packageQuantity : null;
            default -> null;
        };
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

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal base) {
        return amount.multiply(ONE_HUNDRED, MATH)
                .divide(base, MATH)
                .setScale(1, RoundingMode.HALF_UP);
    }

    private static final class Aggregate {
        private final UUID productId;
        private final String productName;
        private final String brand;
        private final UUID categoryId;
        private final String categoryName;
        private final String measurementType;
        private final String requiredUnit;
        private final BigDecimal packageQuantity;
        private final String packageUnit;
        private final BigDecimal packagePrice;
        private final BigDecimal unitPrice;
        private final Boolean available;
        private final LinkedHashSet<String> warnings = new LinkedHashSet<>();
        private BigDecimal requiredQuantity;

        private Aggregate(GeneratedMealPlanResult.IngredientSummary ingredient) {
            this.productId = ingredient.productId();
            this.productName = ingredient.productName() == null
                    ? "Producto sin nombre"
                    : ingredient.productName();
            this.brand = ingredient.brand();
            this.categoryId = ingredient.categoryId();
            this.categoryName = ingredient.categoryName();
            this.measurementType = ingredient.measurementType();
            this.requiredUnit = ingredient.quantityUnit();
            this.packageQuantity = ingredient.packageQuantity();
            this.packageUnit = ingredient.packageUnit();
            this.packagePrice = ingredient.packagePrice();
            this.unitPrice = ingredient.unitPrice();
            this.available = ingredient.available();
            this.requiredQuantity = ingredient.quantity();
            if (ingredient.warnings() != null) {
                this.warnings.addAll(ingredient.warnings());
            }
        }

        private void add(GeneratedMealPlanResult.IngredientSummary ingredient) {
            var units = java.util.Arrays.asList(
                    String.valueOf(requiredUnit),
                    String.valueOf(ingredient.quantityUnit()),
                    String.valueOf(packageUnit),
                    String.valueOf(ingredient.packageUnit())
            );
            if (!java.util.Objects.equals(measurementType, ingredient.measurementType())
                    || !java.util.Objects.equals(requiredUnit, ingredient.quantityUnit())
                    || !java.util.Objects.equals(packageUnit, ingredient.packageUnit())) {
                throw new ShoppingListException(
                        "Incompatible units detected for product " + productName,
                        "SHOPPING_LIST_UNIT_INCOMPATIBLE",
                        422,
                        productId,
                        productName,
                        units,
                        measurementType
                );
            }
            requiredQuantity = requiredQuantity.add(ingredient.quantity(), MATH);
            if (ingredient.warnings() != null) {
                warnings.addAll(ingredient.warnings());
            }
        }
    }

    private record CalculatedItem(
            ShoppingListResponse.Item item,
            List<ShoppingListResponse.ShoppingWarning> warnings
    ) {
    }
}
