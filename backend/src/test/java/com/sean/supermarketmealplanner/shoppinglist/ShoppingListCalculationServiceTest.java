package com.sean.supermarketmealplanner.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus;
import com.sean.supermarketmealplanner.mealtemplate.application.NutritionBreakdown;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListCalculationService;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShoppingListCalculationServiceTest {

    private final ShoppingListCalculationService service = new ShoppingListCalculationService();

    @Test
    void aggregatesProductsAcrossMealsAndDaysAndRoundsPackagesUp() {
        var productId = UUID.randomUUID();
        var result = service.calculate(plan(
                new BigDecimal("20"),
                day(0, ingredient(productId, "Pollo", "600", "GRAM", "WEIGHT", "500", "G", "4")),
                day(1, ingredient(productId, "Pollo", "600", "GRAM", "WEIGHT", "500", "G", "4"))
        ));

        var item = result.items().getFirst();
        assertThat(item.requiredQuantity()).isEqualByComparingTo("1200");
        assertThat(item.packagesRequired()).isEqualTo(3);
        assertThat(item.purchasedQuantity()).isEqualByComparingTo("1500");
        assertThat(item.leftoverQuantity()).isEqualByComparingTo("300");
        assertThat(item.consumedCost()).isEqualByComparingTo("9.60");
        assertThat(item.purchaseCost()).isEqualByComparingTo("12.00");
        assertThat(item.wasteCost()).isEqualByComparingTo("2.40");
        assertThat(item.leftoverPercentage()).isEqualByComparingTo("20.0");
        assertThat(result.purchaseBudgetExceeded()).isFalse();
    }

    @Test
    void calculatesWeightVolumeAndUnitPackagesWithoutMixingSummaries() {
        var result = service.calculate(plan(
                new BigDecimal("7"),
                day(
                        0,
                        ingredient(UUID.randomUUID(), "Arroz", "1000", "GRAM", "WEIGHT", "1", "KG", "2"),
                        ingredient(UUID.randomUUID(), "Leche", "1200", "MILLILITER", "VOLUME", "1", "L", "1.5"),
                        ingredient(UUID.randomUUID(), "Huevos", "13", "UNIT", "UNIT", "12", "UNIT", "2.4")
                )
        ));

        assertThat(result.items()).extracting(item -> item.packagesRequired())
                .containsExactly(1, 2, 2);
        assertThat(result.quantitySummary().get("WEIGHT").required())
                .isEqualByComparingTo("1000");
        assertThat(result.quantitySummary().get("VOLUME").required())
                .isEqualByComparingTo("1200");
        assertThat(result.quantitySummary().get("UNIT").required())
                .isEqualByComparingTo("13");
        assertThat(result.totalPackages()).isEqualTo(5);
        assertThat(result.purchaseBudgetExceeded()).isTrue();
    }

    @Test
    void exactPackageHasNoLeftoverOrWaste() {
        var result = service.calculate(plan(
                null,
                day(0, ingredient(
                        UUID.randomUUID(), "Avena", "500", "GRAM", "WEIGHT", "500", "G", "1.25"
                ))
        ));

        var item = result.items().getFirst();
        assertThat(item.packagesRequired()).isOne();
        assertThat(item.leftoverQuantity()).isEqualByComparingTo("0");
        assertThat(item.leftoverPercentage()).isEqualByComparingTo("0.0");
        assertThat(item.wasteCost()).isEqualByComparingTo("0.00");
    }

    @Test
    void missingPackageSnapshotKeepsProductAsPartialAndUsesNullCalculations() {
        var incomplete = ingredient(
                UUID.randomUUID(), "Producto antiguo", "250", "GRAM", null, null, null, null
        );
        var result = service.calculate(plan(new BigDecimal("10"), day(0, incomplete)));

        var item = result.items().getFirst();
        assertThat(item.requiredQuantity()).isEqualByComparingTo("250");
        assertThat(item.packagesRequired()).isNull();
        assertThat(item.purchaseCost()).isNull();
        assertThat(item.calculationComplete()).isFalse();
        assertThat(result.calculationComplete()).isFalse();
        assertThat(result.budgetCalculationComplete()).isFalse();
        assertThat(result.warnings()).extracting(warning -> warning.code())
                .contains("PRODUCT_SNAPSHOT_INCOMPLETE", "SHOPPING_LIST_CALCULATION_PARTIAL");
    }

    @Test
    void distinguishesMissingPackageDataFromMissingPackagePrice() {
        var missingPackage = ingredient(
                UUID.randomUUID(), "Sin formato", "250", "GRAM", "WEIGHT", null, null, "2"
        );
        var missingPrice = ingredient(
                UUID.randomUUID(), "Sin precio", "250", "GRAM", "WEIGHT", "500", "G", null
        );

        var result = service.calculate(plan(
                new BigDecimal("10"),
                day(0, missingPackage, missingPrice)
        ));

        assertThat(result.items()).allMatch(item -> !item.calculationComplete());
        assertThat(result.items()).flatExtracting(item -> item.warnings())
                .anyMatch(value -> value.startsWith("PACKAGE_DATA_MISSING"))
                .anyMatch(value -> value.startsWith("PACKAGE_PRICE_MISSING"));
    }

    @Test
    void unavailableProductRemainsVisibleWithWarning() {
        var ingredient = ingredient(
                UUID.randomUUID(), "No disponible", "100", "GRAM", "WEIGHT", "500", "G", "4"
        );
        ingredient = new GeneratedMealPlanResult.IngredientSummary(
                ingredient.productId(), ingredient.productName(), ingredient.brand(),
                ingredient.categoryId(), ingredient.categoryName(), ingredient.quantity(),
                ingredient.quantityUnit(), ingredient.measurementType(),
                ingredient.packageQuantity(), ingredient.packageUnit(), ingredient.packagePrice(),
                ingredient.unitPrice(), false, ingredient.consumedCost(), true, List.of(),
                "MEAL_TOTAL"
        );

        var result = service.calculate(plan(null, day(0, ingredient)));

        assertThat(result.items().getFirst().available()).isFalse();
        assertThat(result.items().getFirst().warnings())
                .anyMatch(value -> value.startsWith("PRODUCT_UNAVAILABLE"));
    }

    @Test
    void incompatibleUnitsForSameProductFailWithSafeContext() {
        var productId = UUID.randomUUID();
        var weight = ingredient(
                productId, "Producto", "100", "GRAM", "WEIGHT", "500", "G", "4"
        );
        var incompatible = ingredient(
                productId, "Producto", "1", "UNIT", "UNIT", "1", "UNIT", "4"
        );

        assertThatThrownBy(() -> service.calculate(plan(
                null,
                day(0, weight, incompatible)
        ))).isInstanceOfSatisfying(ShoppingListException.class, exception -> {
            assertThat(exception.errorCode()).isEqualTo("SHOPPING_LIST_UNIT_INCOMPATIBLE");
            assertThat(exception.productId()).isEqualTo(productId);
            assertThat(exception.unitsDetected()).contains("GRAM", "UNIT");
        });
    }

    private GeneratedMealPlanResult plan(
            BigDecimal budget,
            GeneratedMealPlanResult.DayResult... days
    ) {
        var now = OffsetDateTime.parse("2026-08-03T10:00:00Z");
        return new GeneratedMealPlanResult(
                true,
                UUID.randomUUID(),
                "token",
                "Plan",
                "MERCADONA",
                "Mercadona",
                LocalDate.of(2026, 8, 3),
                days.length,
                1,
                1,
                1L,
                GenerationStrategy.SCORING,
                MealPlanStatus.GENERATED,
                null,
                List.of(days),
                zeroNutrition(),
                BigDecimal.ZERO,
                budget,
                null,
                false,
                null,
                BigDecimal.ZERO,
                null,
                null,
                true,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                null,
                now,
                now
        );
    }

    private GeneratedMealPlanResult.DayResult day(
            int dayIndex,
            GeneratedMealPlanResult.IngredientSummary... ingredients
    ) {
        return new GeneratedMealPlanResult.DayResult(
                dayIndex,
                LocalDate.of(2026, 8, 3).plusDays(dayIndex),
                List.of(new GeneratedMealPlanResult.PlannedMealResult(
                        0,
                        "LUNCH",
                        UUID.randomUUID(),
                        "Comida",
                        1,
                        10,
                        List.of(ingredients),
                        zeroNutrition(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        true,
                        List.of()
                )),
                zeroNutrition(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of()
        );
    }

    private GeneratedMealPlanResult.IngredientSummary ingredient(
            UUID id,
            String name,
            String quantity,
            String requiredUnit,
            String measurementType,
            String packageQuantity,
            String packageUnit,
            String price
    ) {
        return new GeneratedMealPlanResult.IngredientSummary(
                id,
                name,
                "Marca",
                UUID.randomUUID(),
                "Categoría",
                new BigDecimal(quantity),
                requiredUnit,
                measurementType,
                packageQuantity == null ? null : new BigDecimal(packageQuantity),
                packageUnit,
                price == null ? null : new BigDecimal(price),
                null,
                true,
                null,
                price != null,
                List.of(),
                "MEAL_TOTAL"
        );
    }

    private NutritionBreakdown zeroNutrition() {
        return new NutritionBreakdown(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}
