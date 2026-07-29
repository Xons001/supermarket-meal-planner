package com.sean.supermarketmealplanner.shared.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sean.supermarketmealplanner.shared.application.purchase.PurchaseMetricsCalculator;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseMetricsCalculatorTest {

    private final PurchaseMetricsCalculator calculator = new PurchaseMetricsCalculator();

    @Test
    void reusingAlreadyPurchasedCapacityHasZeroPurchaseDeltaAndNegativeWasteDelta() {
        var product = UUID.randomUUID();
        var initial = calculator.add(
                calculator.empty(new BigDecimal("20")),
                List.of(weight(product, "300", "500", "5")),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );
        var reused = calculator.add(
                initial.state(),
                List.of(weight(product, "100", "500", "5")),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );

        assertThat(reused.purchaseCostDelta()).isEqualByComparingTo("0.00");
        assertThat(reused.wasteCostDelta()).isEqualByComparingTo("-1.00");
        assertThat(reused.additionalPackages()).isZero();
        assertThat(reused.economicallyUsefulReuse()).isEqualTo(1);
    }

    @Test
    void crossingPackageBoundaryAddsOnePackageAndPositiveWaste() {
        var product = UUID.randomUUID();
        var initial = calculator.add(
                calculator.empty(null),
                List.of(weight(product, "400", "500", "5")),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );
        var crossed = calculator.add(
                initial.state(),
                List.of(weight(product, "200", "500", "5")),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );

        assertThat(crossed.additionalPackages()).isEqualTo(1);
        assertThat(crossed.purchaseCostDelta()).isEqualByComparingTo("5.00");
        assertThat(crossed.wasteCostDelta()).isEqualByComparingTo("3.00");
    }

    @Test
    void convertsKilogramsLitresAndUnitsAndRoundsPackagesUp() {
        var result = calculator.calculate(
                List.of(
                        input(UUID.randomUUID(), "WEIGHT", "GRAM", "1200", "1", "KG", "2"),
                        input(UUID.randomUUID(), "VOLUME", "MILLILITER", "1250", "1", "L", "1.5"),
                        input(UUID.randomUUID(), "UNIT", "UNIT", "13", "12", "UNIT", "2.4")
                ),
                null,
                PurchaseMetricsCalculator.ConflictMode.STRICT
        );

        assertThat(result.lines()).extracting(PurchaseMetricsCalculator.ProductLine::packagesRequired)
                .containsExactlyInAnyOrder(2, 2, 2);
        assertThat(result.totalPackages()).isEqualTo(6);
    }

    @Test
    void incompatibleUnitsArePartialInLenientModeAndRejectedInStrictMode() {
        var product = UUID.randomUUID();
        var values = List.of(
                weight(product, "100", "500", "5"),
                input(product, "UNIT", "UNIT", "1", "12", "UNIT", "2.4")
        );

        var lenient = calculator.calculate(
                values,
                null,
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );
        assertThat(lenient.calculationComplete()).isFalse();
        assertThat(lenient.lines().getFirst().purchaseCost()).isNull();
        assertThat(lenient.warnings()).extracting(PurchaseMetricsCalculator.PurchaseWarning::code)
                .contains("SHOPPING_LIST_UNIT_INCOMPATIBLE");

        assertThatThrownBy(() -> calculator.calculate(
                values,
                null,
                PurchaseMetricsCalculator.ConflictMode.STRICT
        )).isInstanceOf(PurchaseMetricsCalculator.PurchaseUnitConflictException.class);
    }

    @Test
    void incompleteSnapshotsExposeNullsAndStableWarningsWithoutInventingCosts() {
        var incomplete = new PurchaseMetricsCalculator.IngredientInput(
                UUID.randomUUID(),
                "Sin precio",
                null,
                null,
                null,
                new BigDecimal("100"),
                "GRAM",
                "WEIGHT",
                new BigDecimal("500"),
                "G",
                null,
                null,
                null,
                List.of()
        );

        var result = calculator.calculate(
                List.of(incomplete),
                new BigDecimal("20"),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );

        assertThat(result.calculationComplete()).isFalse();
        assertThat(result.lines().getFirst().purchaseCost()).isNull();
        assertThat(result.lines().getFirst().wasteCost()).isNull();
        assertThat(result.warnings()).extracting(PurchaseMetricsCalculator.PurchaseWarning::code)
                .contains("PACKAGE_PRICE_MISSING", "SHOPPING_LIST_CALCULATION_PARTIAL");
    }

    private PurchaseMetricsCalculator.IngredientInput weight(
            UUID productId,
            String quantity,
            String packageQuantity,
            String packagePrice
    ) {
        return input(
                productId,
                "WEIGHT",
                "GRAM",
                quantity,
                packageQuantity,
                "G",
                packagePrice
        );
    }

    private PurchaseMetricsCalculator.IngredientInput input(
            UUID productId,
            String measurementType,
            String requiredUnit,
            String quantity,
            String packageQuantity,
            String packageUnit,
            String packagePrice
    ) {
        return new PurchaseMetricsCalculator.IngredientInput(
                productId,
                "Producto " + productId,
                "Demo",
                null,
                "Categoría",
                new BigDecimal(quantity),
                requiredUnit,
                measurementType,
                new BigDecimal(packageQuantity),
                packageUnit,
                new BigDecimal(packagePrice),
                null,
                true,
                List.of()
        );
    }
}
