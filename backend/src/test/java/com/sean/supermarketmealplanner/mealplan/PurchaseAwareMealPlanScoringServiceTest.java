package com.sean.supermarketmealplanner.mealplan;

import static org.assertj.core.api.Assertions.assertThat;

import com.sean.supermarketmealplanner.mealplan.application.GenerateMealPlanCommand;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanScoringService;
import com.sean.supermarketmealplanner.mealplan.application.PurchaseAwareMealPlanScoringService;
import com.sean.supermarketmealplanner.mealplan.application.PurchaseAwareScoringProperties;
import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealplan.domain.OptimizationPreset;
import com.sean.supermarketmealplanner.mealplan.domain.VarietyPreference;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import com.sean.supermarketmealplanner.shared.application.purchase.PurchaseMetricsCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseAwareMealPlanScoringServiceTest {

    private final PurchaseMetricsCalculator calculator = new PurchaseMetricsCalculator();
    private final PurchaseAwareMealPlanScoringService service =
            new PurchaseAwareMealPlanScoringService(new PurchaseAwareScoringProperties());

    @Test
    void exactBudgetKeepsMaximumBudgetScoreWithoutDominatingNutrition() {
        var purchase = calculator.calculate(
                List.of(ingredient("500", "500", "10")),
                new BigDecimal("10"),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );

        var score = service.score(command("10"), legacy(), purchase, 0);

        assertThat(score.purchaseBudgetScore()).isEqualByComparingTo("100.00");
        assertThat(score.purchaseCostScore()).isEqualByComparingTo("65.00");
        assertThat(score.totalScore()).isGreaterThan(new BigDecimal("80"));
    }

    @Test
    void realPurchaseAndWasteMakeInefficientPackageWorseAtEqualNutrition() {
        var inefficient = calculator.calculate(
                List.of(ingredient("100", "500", "5")),
                new BigDecimal("10"),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );
        var efficient = calculator.calculate(
                List.of(ingredient("100", "100", "1")),
                new BigDecimal("10"),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );

        assertThat(service.score(command("10"), legacy(), efficient, 0).totalScore())
                .isGreaterThan(service.score(command("10"), legacy(), inefficient, 0).totalScore());
    }

    @Test
    void negativeWasteDeltaCreatesAValidMarginalBonus() {
        var initial = calculator.add(
                calculator.empty(new BigDecimal("20")),
                List.of(ingredient("300", "500", "5")),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );
        var reused = calculator.add(
                initial.state(),
                List.of(ingredient("100", "500", "5")),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );

        assertThat(reused.wasteCostDelta()).isNegative();
        assertThat(service.marginalBonus(command("20"), reused)).isPositive();
    }

    @Test
    void usefulReuseEventsAreCappedSoTheyCannotPushTheScoreAboveOneHundred() {
        var purchase = calculator.calculate(
                List.of(
                        ingredient("100", "500", "5"),
                        ingredient("100", "500", "5")
                ),
                new BigDecimal("20"),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );

        var score = service.score(command("20"), legacy(), purchase, 5);

        assertThat(score.usefulReuseScore()).isEqualByComparingTo("100.00");
        assertThat(score.totalScore()).isLessThanOrEqualTo(new BigDecimal("100.00"));
    }

    private MealPlanScoringService.ScoreOutput legacy() {
        var hundred = new BigDecimal("100");
        return new MealPlanScoringService.ScoreOutput(
                hundred,
                hundred,
                hundred,
                hundred,
                hundred,
                hundred,
                hundred,
                hundred,
                1,
                0,
                1,
                hundred,
                Map.of()
        );
    }

    private GenerateMealPlanCommand command(String budget) {
        return new GenerateMealPlanCommand(
                "MERCADONA",
                "Compra eficiente",
                LocalDate.of(2026, 8, 3),
                1,
                1,
                1,
                new BigDecimal("2000"),
                new BigDecimal("100"),
                new BigDecimal(budget),
                Set.of(MealType.LUNCH),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                40,
                3,
                VarietyPreference.HIGH,
                false,
                GenerationStrategy.PURCHASE_AWARE_SCORING,
                OptimizationPreset.BALANCED,
                123L,
                null,
                false
        );
    }

    private PurchaseMetricsCalculator.IngredientInput ingredient(
            String quantity,
            String packageQuantity,
            String packagePrice
    ) {
        return new PurchaseMetricsCalculator.IngredientInput(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Producto",
                "Demo",
                null,
                "Categoría",
                new BigDecimal(quantity),
                "GRAM",
                "WEIGHT",
                new BigDecimal(packageQuantity),
                "G",
                new BigDecimal(packagePrice),
                null,
                true,
                List.of()
        );
    }
}
