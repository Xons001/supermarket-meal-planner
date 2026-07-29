package com.sean.supermarketmealplanner.shared.application.purchase;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Pure package-purchase calculator shared by meal-plan optimization and shopping lists.
 */
@Component
public class PurchaseMetricsCalculator {

    private static final MathContext MATH = MathContext.DECIMAL128;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public PurchaseState empty(BigDecimal weeklyBudget) {
        return new PurchaseState(Map.of(), totals(List.of(), weeklyBudget), weeklyBudget);
    }

    public PurchaseDelta add(
            PurchaseState before,
            List<IngredientInput> ingredients,
            ConflictMode conflictMode
    ) {
        var aggregates = new LinkedHashMap<UUID, ProductAggregate>(before.aggregates());
        var touched = new LinkedHashSet<UUID>();
        for (var ingredient : ingredients) {
            validate(ingredient);
            touched.add(ingredient.productId());
            var current = aggregates.get(ingredient.productId());
            aggregates.put(
                    ingredient.productId(),
                    current == null
                            ? ProductAggregate.first(ingredient)
                            : current.add(ingredient, conflictMode)
            );
        }
        var lines = aggregates.values().stream().map(this::calculateLine).toList();
        var afterCalculation = totals(lines, before.weeklyBudget());
        var after = new PurchaseState(
                java.util.Collections.unmodifiableMap(aggregates),
                afterCalculation,
                before.weeklyBudget()
        );

        var usefulReuse = 0;
        for (var productId : touched) {
            var oldAggregate = before.aggregates().get(productId);
            if (oldAggregate == null) {
                continue;
            }
            var oldLine = calculateLine(oldAggregate);
            var newLine = calculateLine(aggregates.get(productId));
            if (!oldLine.calculationComplete() || !newLine.calculationComplete()) {
                continue;
            }
            var packageDelta = newLine.packagesRequired() - oldLine.packagesRequired();
            var wasteDelta = newLine.wasteCost().subtract(oldLine.wasteCost(), MATH);
            var utilizationImproved = newLine.utilizationPercentage()
                    .compareTo(oldLine.utilizationPercentage()) > 0;
            if (packageDelta == 0 || wasteDelta.signum() < 0 || utilizationImproved) {
                usefulReuse++;
            }
        }

        return new PurchaseDelta(
                after,
                money(afterCalculation.totalPurchaseCost()
                        .subtract(before.calculation().totalPurchaseCost(), MATH)),
                money(afterCalculation.totalWasteCost()
                        .subtract(before.calculation().totalWasteCost(), MATH)),
                afterCalculation.totalPackages() - before.calculation().totalPackages(),
                (int) touched.stream().filter(id -> !before.aggregates().containsKey(id)).count(),
                usefulReuse
        );
    }

    public PurchaseCalculation calculate(
            List<IngredientInput> ingredients,
            BigDecimal weeklyBudget,
            ConflictMode conflictMode
    ) {
        var state = empty(weeklyBudget);
        for (var ingredient : ingredients) {
            state = add(state, List.of(ingredient), conflictMode).state();
        }
        return state.calculation();
    }

    private void validate(IngredientInput ingredient) {
        if (ingredient.productId() == null) {
            throw new InvalidPurchaseInputException(
                    "A planned ingredient has no productId snapshot",
                    "PRODUCT_SNAPSHOT_INCOMPLETE",
                    null
            );
        }
        if (ingredient.quantity() == null || ingredient.quantity().signum() < 0) {
            throw new InvalidPurchaseInputException(
                    "Invalid required quantity for product " + ingredient.productId(),
                    "INVALID_REQUIRED_QUANTITY",
                    ingredient.productId()
            );
        }
    }

    private ProductLine calculateLine(ProductAggregate aggregate) {
        var warnings = new LinkedHashSet<String>(aggregate.warnings());
        if (Boolean.FALSE.equals(aggregate.available())) {
            warnings.add("PRODUCT_UNAVAILABLE: Product was unavailable when the plan was generated");
        }
        String missingCode = null;
        var packageBase = packageBaseQuantity(
                aggregate.packageQuantity(),
                aggregate.packageUnit(),
                aggregate.measurementType()
        );
        if (aggregate.conflictingUnits()
                || aggregate.measurementType() == null
                || aggregate.requiredUnit() == null) {
            missingCode = aggregate.conflictingUnits()
                    ? "SHOPPING_LIST_UNIT_INCOMPATIBLE"
                    : "PRODUCT_SNAPSHOT_INCOMPLETE";
        } else if (aggregate.packageQuantity() == null || aggregate.packageUnit() == null) {
            missingCode = "PACKAGE_DATA_MISSING";
        } else if (aggregate.packagePrice() == null) {
            missingCode = "PACKAGE_PRICE_MISSING";
        } else if (packageBase == null) {
            missingCode = "PACKAGE_UNIT_INCOMPATIBLE";
        } else if (aggregate.packageQuantity().signum() <= 0
                || aggregate.packagePrice().signum() < 0) {
            missingCode = "PACKAGE_DATA_INVALID";
        }

        Integer packages = null;
        BigDecimal purchased = null;
        BigDecimal leftover = null;
        BigDecimal consumed = null;
        BigDecimal purchase = null;
        BigDecimal waste = null;
        BigDecimal wastePercentage = null;
        BigDecimal utilizationPercentage = null;
        var complete = missingCode == null;
        if (complete) {
            packages = aggregate.requiredQuantity().divide(
                    packageBase,
                    0,
                    RoundingMode.CEILING
            ).intValueExact();
            purchased = quantity(packageBase.multiply(BigDecimal.valueOf(packages), MATH));
            leftover = quantity(purchased.subtract(aggregate.requiredQuantity(), MATH));
            purchase = money(aggregate.packagePrice()
                    .multiply(BigDecimal.valueOf(packages), MATH));
            consumed = money(aggregate.packagePrice()
                    .multiply(aggregate.requiredQuantity(), MATH)
                    .divide(packageBase, MATH));
            waste = money(purchase.subtract(consumed, MATH));
            wastePercentage = purchased.signum() == 0
                    ? BigDecimal.ZERO.setScale(1)
                    : percentage(leftover, purchased);
            utilizationPercentage = ONE_HUNDRED.subtract(wastePercentage)
                    .setScale(1, RoundingMode.HALF_UP);
        } else {
            warnings.add(missingCode + ": Package calculation is unavailable for this snapshot");
        }

        return new ProductLine(
                aggregate.productId(),
                aggregate.productName(),
                aggregate.brand(),
                aggregate.categoryId(),
                aggregate.categoryName(),
                aggregate.measurementType(),
                aggregate.requiredQuantity() == null ? null : quantity(aggregate.requiredQuantity()),
                aggregate.requiredUnit(),
                aggregate.packageQuantity(),
                aggregate.packageUnit(),
                aggregate.packagePrice(),
                aggregate.unitPrice(),
                packages,
                purchased,
                leftover,
                consumed,
                purchase,
                waste,
                wastePercentage,
                utilizationPercentage,
                aggregate.available(),
                complete,
                aggregate.mealOccurrences(),
                List.copyOf(warnings)
        );
    }

    private PurchaseCalculation totals(List<ProductLine> unsorted, BigDecimal weeklyBudget) {
        var lines = unsorted.stream()
                .sorted(Comparator.comparing(
                                ProductLine::categoryName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                        .thenComparing(ProductLine::productName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ProductLine::productId))
                .toList();
        var calculable = lines.stream().filter(ProductLine::calculationComplete).toList();
        var consumed = money(calculable.stream()
                .map(ProductLine::consumedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        var purchase = money(calculable.stream()
                .map(ProductLine::purchaseCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        var waste = money(calculable.stream()
                .map(ProductLine::wasteCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        var totalPackages = calculable.stream().mapToInt(ProductLine::packagesRequired).sum();
        var complete = lines.stream().allMatch(ProductLine::calculationComplete);
        var overallWaste = purchase.signum() == 0
                ? BigDecimal.ZERO.setScale(1)
                : percentage(waste, purchase);
        var difference = weeklyBudget == null ? null : money(weeklyBudget.subtract(purchase));
        var exceeded = weeklyBudget != null && purchase.compareTo(weeklyBudget) > 0;
        var deviation = weeklyBudget == null
                ? null
                : percentage(purchase.subtract(weeklyBudget).abs(), weeklyBudget);
        var warnings = new ArrayList<PurchaseWarning>();
        lines.forEach(line -> line.warnings().forEach(warning -> warnings.add(new PurchaseWarning(
                warningCode(warning),
                warning,
                line.productId()
        ))));
        if (!complete) {
            warnings.add(new PurchaseWarning(
                    "SHOPPING_LIST_CALCULATION_PARTIAL",
                    "Purchase cost and budget comparison are partial because some products "
                            + "could not be calculated",
                    null
            ));
        }
        return new PurchaseCalculation(
                List.copyOf(lines),
                totalPackages,
                consumed,
                purchase,
                waste,
                overallWaste,
                weeklyBudget,
                difference,
                exceeded,
                deviation,
                complete,
                lines.size(),
                (int) lines.stream().filter(line -> line.mealOccurrences() > 1).count(),
                List.copyOf(warnings)
        );
    }

    private String warningCode(String warning) {
        var separator = warning.indexOf(':');
        return separator > 0
                ? warning.substring(0, separator).trim().replace(' ', '_').toUpperCase()
                : "PRODUCT_WARNING";
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

    private BigDecimal quantity(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal base) {
        if (base.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return amount.multiply(ONE_HUNDRED, MATH)
                .divide(base, MATH)
                .setScale(1, RoundingMode.HALF_UP);
    }

    public enum ConflictMode {
        STRICT,
        LENIENT
    }

    public record IngredientInput(
            UUID productId,
            String productName,
            String brand,
            UUID categoryId,
            String categoryName,
            BigDecimal quantity,
            String quantityUnit,
            String measurementType,
            BigDecimal packageQuantity,
            String packageUnit,
            BigDecimal packagePrice,
            BigDecimal unitPrice,
            Boolean available,
            List<String> warnings
    ) {
        public IngredientInput {
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    public record ProductLine(
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
            BigDecimal wastePercentage,
            BigDecimal utilizationPercentage,
            Boolean available,
            boolean calculationComplete,
            int mealOccurrences,
            List<String> warnings
    ) {
    }

    public record PurchaseWarning(String code, String message, UUID productId) {
    }

    public record PurchaseCalculation(
            List<ProductLine> lines,
            int totalPackages,
            BigDecimal totalConsumedCost,
            BigDecimal totalPurchaseCost,
            BigDecimal totalWasteCost,
            BigDecimal wastePercentage,
            BigDecimal weeklyBudget,
            BigDecimal purchaseBudgetDifference,
            boolean purchaseBudgetExceeded,
            BigDecimal purchaseBudgetDeviationPercentage,
            boolean calculationComplete,
            int uniqueProductCount,
            int reusedProductCount,
            List<PurchaseWarning> warnings
    ) {
    }

    public record PurchaseState(
            Map<UUID, ProductAggregate> aggregates,
            PurchaseCalculation calculation,
            BigDecimal weeklyBudget
    ) {
    }

    public record PurchaseDelta(
            PurchaseState state,
            BigDecimal purchaseCostDelta,
            BigDecimal wasteCostDelta,
            int additionalPackages,
            int newProducts,
            int economicallyUsefulReuse
    ) {
    }

    public static final class PurchaseUnitConflictException extends RuntimeException {
        private final UUID productId;
        private final String productName;
        private final List<String> units;
        private final String measurementType;

        private PurchaseUnitConflictException(
                UUID productId,
                String productName,
                List<String> units,
                String measurementType
        ) {
            super("Incompatible units detected for product " + productName);
            this.productId = productId;
            this.productName = productName;
            this.units = List.copyOf(units);
            this.measurementType = measurementType;
        }

        public UUID productId() { return productId; }
        public String productName() { return productName; }
        public List<String> units() { return units; }
        public String measurementType() { return measurementType; }
    }

    public static final class InvalidPurchaseInputException extends RuntimeException {
        private final String code;
        private final UUID productId;

        private InvalidPurchaseInputException(String message, String code, UUID productId) {
            super(message);
            this.code = code;
            this.productId = productId;
        }

        public String code() { return code; }
        public UUID productId() { return productId; }
    }

    public record ProductAggregate(
            UUID productId,
            String productName,
            String brand,
            UUID categoryId,
            String categoryName,
            String measurementType,
            String requiredUnit,
            BigDecimal packageQuantity,
            String packageUnit,
            BigDecimal packagePrice,
            BigDecimal unitPrice,
            Boolean available,
            BigDecimal requiredQuantity,
            int mealOccurrences,
            boolean conflictingUnits,
            Set<String> warnings
    ) {
        static ProductAggregate first(IngredientInput ingredient) {
            return new ProductAggregate(
                    ingredient.productId(),
                    ingredient.productName() == null ? "Producto sin nombre" : ingredient.productName(),
                    ingredient.brand(),
                    ingredient.categoryId(),
                    ingredient.categoryName(),
                    ingredient.measurementType(),
                    ingredient.quantityUnit(),
                    ingredient.packageQuantity(),
                    ingredient.packageUnit(),
                    ingredient.packagePrice(),
                    ingredient.unitPrice(),
                    ingredient.available(),
                    ingredient.quantity(),
                    1,
                    false,
                    new LinkedHashSet<>(ingredient.warnings())
            );
        }

        ProductAggregate add(IngredientInput ingredient, ConflictMode mode) {
            var incompatible = !Objects.equals(measurementType, ingredient.measurementType())
                    || !Objects.equals(requiredUnit, ingredient.quantityUnit())
                    || !Objects.equals(packageUnit, ingredient.packageUnit());
            if (incompatible && mode == ConflictMode.STRICT) {
                throw new PurchaseUnitConflictException(
                        productId,
                        productName,
                        List.of(
                                String.valueOf(requiredUnit),
                                String.valueOf(ingredient.quantityUnit()),
                                String.valueOf(packageUnit),
                                String.valueOf(ingredient.packageUnit())
                        ),
                        measurementType
                );
            }
            var combinedWarnings = new LinkedHashSet<>(warnings);
            combinedWarnings.addAll(ingredient.warnings());
            if (incompatible) {
                combinedWarnings.add(
                        "SHOPPING_LIST_UNIT_INCOMPATIBLE: Incompatible units detected for product "
                                + productName
                );
            }
            return new ProductAggregate(
                    productId,
                    productName,
                    brand,
                    categoryId,
                    categoryName,
                    measurementType,
                    requiredUnit,
                    packageQuantity,
                    packageUnit,
                    packagePrice,
                    unitPrice,
                    available,
                    incompatible || conflictingUnits
                            ? requiredQuantity
                            : requiredQuantity.add(ingredient.quantity(), MATH),
                    mealOccurrences + 1,
                    conflictingUnits || incompatible,
                    java.util.Collections.unmodifiableSet(combinedWarnings)
            );
        }
    }
}
