package com.sean.supermarketmealplanner.mealplan.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sean.supermarketmealplanner.mealplan.domain.VarietyPreference;
import com.sean.supermarketmealplanner.mealtemplate.application.NutritionBreakdown;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MealPlanScoringServiceTest {

    private final MealPlanScoringService service =
            new MealPlanScoringService(new MealPlanScoringProperties());

    @Test
    void givesMaximumNutritionScoresInsideTargets() {
        var output = service.score(command("2000", "100", "70", 3), List.of(
                meal(0, 0, UUID.randomUUID(), "2000", "110", "8", 20, true)
        ));

        assertThat(output.calorieScore()).isGreaterThan(new BigDecimal("79"));
        assertThat(output.proteinScore()).isEqualByComparingTo("100.00");
        assertThat(output.budgetScore()).isEqualByComparingTo("100.00");
        assertThat(output.completenessScore()).isEqualByComparingTo("100.00");
    }

    @Test
    void penalizesBudgetRepetitionIncompleteDataAndPreparation() {
        var repeatedId = UUID.randomUUID();
        var output = service.score(command("2000", "100", "5", 1), List.of(
                meal(0, 0, repeatedId, "900", "20", "8", 80, false),
                meal(0, 1, repeatedId, "900", "20", "8", 80, true),
                meal(1, 0, repeatedId, "900", "20", "8", 80, true)
        ));

        assertThat(output.budgetScore()).isLessThan(new BigDecimal("100"));
        assertThat(output.repetitionScore()).isLessThan(new BigDecimal("50"));
        assertThat(output.completenessScore()).isEqualByComparingTo("80.00");
        assertThat(output.preparationScore()).isLessThan(new BigDecimal("100"));
        assertThat(output.maximumObservedRepetition()).isEqualTo(3);
    }

    @Test
    void configurableWeightsChangeTheTotalWithoutChangingFactorScores() {
        var properties = new MealPlanScoringProperties();
        var baseline = new MealPlanScoringService(properties);
        var meals = List.of(meal(
                0, 0, UUID.randomUUID(), "1000", "100", "2", 10, true
        ));
        var command = command("2000", "100", "70", 3);
        var initial = baseline.score(command, meals);

        properties.setCalorieWeight(new BigDecimal("100"));
        properties.setProteinWeight(BigDecimal.ZERO);
        properties.setBudgetWeight(BigDecimal.ZERO);
        properties.setVarietyWeight(BigDecimal.ZERO);
        properties.setRepetitionWeight(BigDecimal.ZERO);
        properties.setCompletenessWeight(BigDecimal.ZERO);
        properties.setPreparationWeight(BigDecimal.ZERO);
        var calorieOnly = baseline.score(command, meals);

        assertThat(calorieOnly.calorieScore()).isEqualByComparingTo(initial.calorieScore());
        assertThat(calorieOnly.totalScore()).isEqualByComparingTo(calorieOnly.calorieScore());
        assertThat(calorieOnly.totalScore()).isNotEqualByComparingTo(initial.totalScore());
    }

    @Test
    void differentSeedsCanChangeTheOrderWhenCandidateScoresTie() {
        var first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var observedOrders = new java.util.HashSet<Boolean>();

        for (long seed = 1; seed <= 100; seed++) {
            observedOrders.add(Long.compareUnsigned(
                    ScoringMealPlanGenerationStrategy.tieKey(seed, 0, first),
                    ScoringMealPlanGenerationStrategy.tieKey(seed, 0, second)
            ) < 0);
        }

        assertThat(observedOrders).containsExactlyInAnyOrder(true, false);
    }

    @Test
    void highVarietyDemandsMoreUniqueTemplatesThanLowVariety() {
        var repeated = UUID.randomUUID();
        var meals = List.of(
                meal(0, 0, repeated, "1000", "50", "2", 20, true),
                meal(0, 1, repeated, "1000", "50", "2", 20, true)
        );

        var low = service.score(commandWithVariety(VarietyPreference.LOW, 4), meals);
        var high = service.score(commandWithVariety(VarietyPreference.HIGH, 2), meals);

        assertThat(low.varietyScore()).isGreaterThan(high.varietyScore());
    }

    @Test
    void consecutiveRepetitionIsPenalizedMoreThanSeparatedUse() {
        var repeated = UUID.randomUUID();
        var consecutive = service.score(command("2000", "100", "70", 4), List.of(
                meal(0, 0, repeated, "2000", "100", "2", 20, true),
                meal(1, 0, repeated, "2000", "100", "2", 20, true)
        ));
        var separated = service.score(command("2000", "100", "70", 4), List.of(
                meal(0, 0, repeated, "2000", "100", "2", 20, true),
                meal(2, 0, repeated, "2000", "100", "2", 20, true)
        ));

        assertThat(consecutive.repetitionScore()).isLessThan(separated.repetitionScore());
    }

    @Test
    void rewardsIngredientVarietyWhenTemplateVarietyIsEqual() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var repeatedIngredient = UUID.randomUUID();
        var varied = service.score(commandWithVariety(VarietyPreference.HIGH, 2), List.of(
                meal(0, 0, first, Set.of(UUID.randomUUID()), "1000", "50", "2", 20, true),
                meal(0, 1, second, Set.of(UUID.randomUUID()), "1000", "50", "2", 20, true)
        ));
        var repeated = service.score(commandWithVariety(VarietyPreference.HIGH, 2), List.of(
                meal(0, 0, first, Set.of(repeatedIngredient), "1000", "50", "2", 20, true),
                meal(0, 1, second, Set.of(repeatedIngredient), "1000", "50", "2", 20, true)
        ));

        assertThat(varied.varietyScore()).isGreaterThan(repeated.varietyScore());
    }

    @Test
    void rewardsBalancedCalorieDistributionWithEqualDailyTotal() {
        var balanced = service.score(commandWithVariety(VarietyPreference.HIGH, 2), List.of(
                meal(0, 0, UUID.randomUUID(), "1000", "50", "2", 20, true),
                meal(0, 1, UUID.randomUUID(), "1000", "50", "2", 20, true)
        ));
        var concentrated = service.score(commandWithVariety(VarietyPreference.HIGH, 2), List.of(
                meal(0, 0, UUID.randomUUID(), "1800", "50", "2", 20, true),
                meal(0, 1, UUID.randomUUID(), "200", "50", "2", 20, true)
        ));

        assertThat(balanced.calorieScore()).isGreaterThan(concentrated.calorieScore());
    }

    private GenerateMealPlanCommand commandWithVariety(
            VarietyPreference preference,
            int maxRepetitions
    ) {
        return new GenerateMealPlanCommand(
                "MERCADONA",
                "Plan unitario",
                LocalDate.of(2026, 8, 3),
                1,
                2,
                1,
                new BigDecimal("2000"),
                new BigDecimal("100"),
                new BigDecimal("70"),
                Set.of(MealType.LUNCH),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                40,
                maxRepetitions,
                preference,
                false,
                123L,
                null,
                false
        );
    }

    private GenerateMealPlanCommand command(
            String calories,
            String protein,
            String budget,
            int maxRepetitions
    ) {
        return new GenerateMealPlanCommand(
                "MERCADONA",
                "Plan unitario",
                LocalDate.of(2026, 8, 3),
                1,
                1,
                1,
                new BigDecimal(calories),
                new BigDecimal(protein),
                new BigDecimal(budget),
                Set.of(MealType.LUNCH),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                40,
                maxRepetitions,
                VarietyPreference.HIGH,
                false,
                123L,
                null,
                false
        );
    }

    private MealPlanScoringService.ScoredMeal meal(
            int day,
            int position,
            UUID templateId,
            String calories,
            String protein,
            String cost,
            int minutes,
            boolean complete
    ) {
        return meal(
                day,
                position,
                templateId,
                Set.of(templateId),
                calories,
                protein,
                cost,
                minutes,
                complete
        );
    }

    private MealPlanScoringService.ScoredMeal meal(
            int day,
            int position,
            UUID templateId,
            Set<UUID> ingredientProductIds,
            String calories,
            String protein,
            String cost,
            int minutes,
            boolean complete
    ) {
        return new MealPlanScoringService.ScoredMeal(
                day,
                position,
                templateId,
                MealType.LUNCH,
                ingredientProductIds,
                new NutritionBreakdown(
                        new BigDecimal(calories),
                        new BigDecimal(protein),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                new BigDecimal(cost),
                minutes,
                complete
        );
    }
}
