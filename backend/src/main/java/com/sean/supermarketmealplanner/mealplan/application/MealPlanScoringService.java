package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealtemplate.application.NutritionBreakdown;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MealPlanScoringService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final MealPlanScoringProperties properties;

    public MealPlanScoringService(MealPlanScoringProperties properties) {
        this.properties = properties;
    }

    public ScoreOutput score(GenerateMealPlanCommand command, List<ScoredMeal> meals) {
        var daily = aggregateByDay(meals);
        var dailyCalorieScore = average(daily.values().stream()
                .map(total -> calorieDayScore(total.nutrition().calories(), command.dailyCaloriesTarget()))
                .toList());
        var caloriePerMealTarget = divide(
                command.dailyCaloriesTarget(),
                BigDecimal.valueOf(command.mealsPerDay())
        );
        var distributionScore = average(meals.stream()
                .map(meal -> calorieDayScore(meal.nutrition().calories(), caloriePerMealTarget))
                .toList());
        var distributionShare = properties.getNutritionalDistributionShare();
        var calorieScore = divide(
                dailyCalorieScore.multiply(ONE_HUNDRED.subtract(distributionShare))
                        .add(distributionScore.multiply(distributionShare)),
                ONE_HUNDRED
        );
        var proteinScore = average(daily.values().stream()
                .map(total -> proteinDayScore(total.nutrition().protein(), command.dailyProteinTarget()))
                .toList());
        var totalCost = meals.stream()
                .map(ScoredMeal::cost)
                .reduce(ZERO, BigDecimal::add);
        var budgetScore = budgetScore(totalCost, command.weeklyBudget());
        var counts = new HashMap<UUID, Integer>();
        meals.forEach(meal -> counts.merge(meal.templateId(), 1, Integer::sum));
        var unique = counts.size();
        var repeated = (int) counts.values().stream().filter(value -> value > 1).count();
        var maximumObserved = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        var uniqueRatio = meals.isEmpty()
                ? ZERO
                : divide(BigDecimal.valueOf(unique), BigDecimal.valueOf(meals.size()));
        var templateVarietyScore = cap100(divide(
                uniqueRatio.multiply(ONE_HUNDRED),
                command.varietyPreference().targetUniqueRatio()
        ));
        var ingredientOccurrences = meals.stream()
                .mapToLong(meal -> meal.ingredientProductIds().size())
                .sum();
        var uniqueIngredients = meals.stream()
                .flatMap(meal -> meal.ingredientProductIds().stream())
                .distinct()
                .count();
        var ingredientVarietyScore = ingredientOccurrences == 0
                ? ZERO
                : percentage(
                        BigDecimal.valueOf(uniqueIngredients),
                        BigDecimal.valueOf(ingredientOccurrences)
                );
        var expectedMealTypes = Math.max(
                1,
                Math.min(command.mealsPerDay(), command.allowedMealTypes().size())
        );
        var uniqueMealTypes = meals.stream().map(ScoredMeal::mealType).distinct().count();
        var mealTypeVarietyScore = cap100(percentage(
                BigDecimal.valueOf(uniqueMealTypes),
                BigDecimal.valueOf(expectedMealTypes)
        ));
        var varietyShares = properties.getTemplateVarietyShare()
                .add(properties.getIngredientVarietyShare())
                .add(properties.getMealTypeVarietyShare());
        var varietyScore = divide(
                templateVarietyScore.multiply(properties.getTemplateVarietyShare())
                        .add(ingredientVarietyScore.multiply(properties.getIngredientVarietyShare()))
                        .add(mealTypeVarietyScore.multiply(properties.getMealTypeVarietyShare())),
                varietyShares
        );
        var repetitionScore = repetitionScore(command, meals, counts);
        var incompleteCount = (int) meals.stream().filter(meal -> !meal.complete()).count();
        var completenessScore = maxZero(ONE_HUNDRED.subtract(
                BigDecimal.valueOf(incompleteCount).multiply(
                        properties.getIncompleteMealPenalty()
                )
        ));
        var preparationScore = preparationScore(command, meals);
        var totalScore = weightedTotal(
                calorieScore,
                proteinScore,
                budgetScore,
                varietyScore,
                repetitionScore,
                completenessScore,
                preparationScore
        );
        var dailyScores = new LinkedHashMap<Integer, BigDecimal>();
        daily.forEach((dayIndex, total) -> dailyScores.put(
                dayIndex,
                average(List.of(
                        calorieDayScore(total.nutrition().calories(), command.dailyCaloriesTarget()),
                        proteinDayScore(total.nutrition().protein(), command.dailyProteinTarget())
                ))
        ));
        return new ScoreOutput(
                scale(calorieScore),
                scale(proteinScore),
                scale(budgetScore),
                scale(varietyScore),
                scale(repetitionScore),
                scale(completenessScore),
                scale(preparationScore),
                scale(totalScore),
                unique,
                repeated,
                maximumObserved,
                scale(varietyScore),
                Map.copyOf(dailyScores)
        );
    }

    BigDecimal calorieDayScore(BigDecimal actual, BigDecimal target) {
        var deviation = percentage(actual.subtract(target).abs(), target);
        var outsideIdeal = maxZero(deviation.subtract(
                properties.getIdealCalorieMarginPercentage()
        ));
        return maxZero(ONE_HUNDRED.subtract(
                outsideIdeal.multiply(properties.getCalorieDeviationPenaltyFactor())
        ));
    }

    BigDecimal proteinDayScore(BigDecimal actual, BigDecimal target) {
        if (target.signum() == 0 || actual.compareTo(target) >= 0) {
            return ONE_HUNDRED;
        }
        var deficit = percentage(target.subtract(actual), target);
        return maxZero(ONE_HUNDRED.subtract(
                deficit.multiply(properties.getProteinDeficitPenaltyFactor())
        ));
    }

    private BigDecimal budgetScore(BigDecimal cost, BigDecimal budget) {
        if (budget == null || cost.compareTo(budget) <= 0) {
            return ONE_HUNDRED;
        }
        var exceeded = percentage(cost.subtract(budget), budget);
        return maxZero(ONE_HUNDRED.subtract(
                exceeded.multiply(properties.getBudgetExceededPenaltyFactor())
        ));
    }

    private BigDecimal repetitionScore(
            GenerateMealPlanCommand command,
            List<ScoredMeal> meals,
            Map<UUID, Integer> counts
    ) {
        var excess = counts.values().stream()
                .mapToInt(count -> Math.max(0, count - command.effectiveMaximumTemplateRepetitions()))
                .sum();
        var sameDay = 0;
        var consecutive = 0;
        for (int index = 0; index < meals.size(); index++) {
            var current = meals.get(index);
            for (int previousIndex = 0; previousIndex < index; previousIndex++) {
                var previous = meals.get(previousIndex);
                if (!previous.templateId().equals(current.templateId())) {
                    continue;
                }
                if (previous.dayIndex() == current.dayIndex()) {
                    sameDay++;
                } else if (previous.dayIndex() + 1 == current.dayIndex()
                        && previous.position() == current.position()) {
                    consecutive++;
                }
            }
        }
        var penalty = BigDecimal.valueOf(excess)
                .multiply(properties.getExcessRepetitionPenalty())
                .add(BigDecimal.valueOf(sameDay)
                        .multiply(properties.getSameDayRepetitionPenalty()))
                .add(BigDecimal.valueOf(consecutive)
                        .multiply(properties.getConsecutiveRepetitionPenalty()));
        return maxZero(ONE_HUNDRED.subtract(penalty));
    }

    private BigDecimal preparationScore(
            GenerateMealPlanCommand command,
            List<ScoredMeal> meals
    ) {
        if (meals.isEmpty()) {
            return ZERO;
        }
        var averageMinutes = BigDecimal.valueOf(
                meals.stream().mapToInt(ScoredMeal::preparationMinutes).average().orElse(0)
        );
        var reference = BigDecimal.valueOf(
                command.maximumPreparationMinutes() == null
                        ? 60
                        : Math.max(1, command.maximumPreparationMinutes())
        );
        return maxZero(ONE_HUNDRED.subtract(
                divide(averageMinutes, reference)
                        .multiply(properties.getPreparationRatioPenalty())
        ));
    }

    private BigDecimal weightedTotal(
            BigDecimal calorie,
            BigDecimal protein,
            BigDecimal budget,
            BigDecimal variety,
            BigDecimal repetition,
            BigDecimal completeness,
            BigDecimal preparation
    ) {
        var weighted = calorie.multiply(properties.getCalorieWeight())
                .add(protein.multiply(properties.getProteinWeight()))
                .add(budget.multiply(properties.getBudgetWeight()))
                .add(variety.multiply(properties.getVarietyWeight()))
                .add(repetition.multiply(properties.getRepetitionWeight()))
                .add(completeness.multiply(properties.getCompletenessWeight()))
                .add(preparation.multiply(properties.getPreparationWeight()));
        return divide(weighted, properties.totalWeight());
    }

    private Map<Integer, DayTotal> aggregateByDay(List<ScoredMeal> meals) {
        var result = new LinkedHashMap<Integer, DayTotal>();
        meals.stream().sorted(java.util.Comparator.comparingInt(ScoredMeal::dayIndex))
                .forEach(meal -> result.merge(
                        meal.dayIndex(),
                        new DayTotal(meal.nutrition(), meal.cost()),
                        DayTotal::add
                ));
        return result;
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return ZERO;
        }
        return divide(values.stream().reduce(ZERO, BigDecimal::add), BigDecimal.valueOf(values.size()));
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal base) {
        return divide(amount.multiply(ONE_HUNDRED), base);
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) {
            return ZERO;
        }
        return numerator.divide(denominator, 12, RoundingMode.HALF_UP);
    }

    private BigDecimal cap100(BigDecimal value) {
        return value.min(ONE_HUNDRED).max(ZERO);
    }

    private BigDecimal maxZero(BigDecimal value) {
        return value.max(ZERO);
    }

    private BigDecimal scale(BigDecimal value) {
        return cap100(value).setScale(2, RoundingMode.HALF_UP);
    }

    public record ScoredMeal(
            int dayIndex,
            int position,
            UUID templateId,
            MealType mealType,
            Set<UUID> ingredientProductIds,
            NutritionBreakdown nutrition,
            BigDecimal cost,
            int preparationMinutes,
            boolean complete
    ) {
    }

    public record ScoreOutput(
            BigDecimal calorieScore,
            BigDecimal proteinScore,
            BigDecimal budgetScore,
            BigDecimal varietyScore,
            BigDecimal repetitionScore,
            BigDecimal completenessScore,
            BigDecimal preparationScore,
            BigDecimal totalScore,
            int uniqueTemplates,
            int repeatedTemplates,
            int maximumObservedRepetition,
            BigDecimal varietyMetricScore,
            Map<Integer, BigDecimal> dailyScores
    ) {
    }

    private record DayTotal(NutritionBreakdown nutrition, BigDecimal cost) {
        DayTotal add(DayTotal other) {
            return new DayTotal(addNutrition(nutrition, other.nutrition), cost.add(other.cost));
        }
    }

    static NutritionBreakdown addNutrition(NutritionBreakdown left, NutritionBreakdown right) {
        return new NutritionBreakdown(
                left.calories().add(right.calories()),
                left.protein().add(right.protein()),
                left.carbohydrates().add(right.carbohydrates()),
                left.fat().add(right.fat()),
                left.fiber().add(right.fiber()),
                left.sugar().add(right.sugar()),
                left.salt().add(right.salt())
        );
    }
}
