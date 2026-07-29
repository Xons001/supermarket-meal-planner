package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealtemplate.application.NutritionBreakdown;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import com.sean.supermarketmealplanner.shared.application.purchase.PurchaseMetricsCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MealPlanRecalculationService {
    private final MealPlanScoringService scoringService;
    private final PurchaseAwareMealPlanScoringService purchaseScoringService;
    private final PurchaseMetricsCalculator purchaseCalculator;

    public MealPlanRecalculationService(
            MealPlanScoringService scoringService,
            PurchaseAwareMealPlanScoringService purchaseScoringService,
            PurchaseMetricsCalculator purchaseCalculator
    ) {
        this.scoringService = scoringService;
        this.purchaseScoringService = purchaseScoringService;
        this.purchaseCalculator = purchaseCalculator;
    }

    public GeneratedMealPlanResult recalculate(
            GeneratedMealPlanResult source,
            List<GeneratedMealPlanResult.DayResult> requestedDays,
            long editVersion,
            long contentVersion,
            OffsetDateTime now
    ) {
        var days = requestedDays.stream()
                .sorted(Comparator.comparingInt(GeneratedMealPlanResult.DayResult::dayIndex))
                .toList();
        var command = command(source);
        var scoredMeals = days.stream().flatMap(day -> day.meals().stream().map(meal ->
                new MealPlanScoringService.ScoredMeal(
                        day.dayIndex(),
                        meal.position(),
                        meal.templateId(),
                        MealType.valueOf(meal.mealType()),
                        meal.ingredients().stream()
                                .map(GeneratedMealPlanResult.IngredientSummary::productId)
                                .collect(Collectors.toSet()),
                        meal.nutrition(),
                        meal.consumedCost(),
                        meal.preparationMinutes(),
                        meal.calculationComplete()
                ))).toList();
        var legacy = scoringService.score(command, scoredMeals);
        var purchase = purchaseCalculator.calculate(
                days.stream().flatMap(day -> day.meals().stream())
                        .flatMap(meal -> meal.ingredients().stream())
                        .map(this::purchaseInput)
                        .toList(),
                source.weeklyBudget(),
                PurchaseMetricsCalculator.ConflictMode.LENIENT
        );
        var usefulReuse = usefulReuse(days, source.weeklyBudget());
        var purchaseScore = source.strategy() == GenerationStrategy.PURCHASE_AWARE_SCORING
                ? purchaseScoringService.score(command, legacy, purchase, usefulReuse)
                : null;
        var recalculatedDays = new ArrayList<GeneratedMealPlanResult.DayResult>();
        var weeklyNutrition = zeroNutrition();
        var weeklyCost = BigDecimal.ZERO;
        for (var day : days) {
            var nutrition = day.meals().stream().map(GeneratedMealPlanResult.PlannedMealResult::nutrition)
                    .reduce(zeroNutrition(), MealPlanScoringService::addNutrition);
            var cost = day.meals().stream()
                    .map(GeneratedMealPlanResult.PlannedMealResult::consumedCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            weeklyNutrition = MealPlanScoringService.addNutrition(weeklyNutrition, nutrition);
            weeklyCost = weeklyCost.add(cost);
            var calorieDeviation = nutrition.calories().subtract(source.criteria().dailyCaloriesTarget()).abs();
            var caloriePct = percentage(calorieDeviation, source.criteria().dailyCaloriesTarget());
            recalculatedDays.add(new GeneratedMealPlanResult.DayResult(
                    day.dayIndex(),
                    day.date(),
                    day.meals(),
                    scaleNutrition(nutrition),
                    money(cost),
                    source.criteria().dailyCaloriesTarget(),
                    source.criteria().dailyProteinTarget(),
                    nutrient(calorieDeviation),
                    caloriePct,
                    nutrient(nutrition.protein().subtract(source.criteria().dailyProteinTarget())),
                    legacy.dailyScores().getOrDefault(day.dayIndex(), BigDecimal.ZERO),
                    day.warnings(),
                    day.dayId()
            ));
        }
        var budgetDifference = source.weeklyBudget() == null
                ? null : source.weeklyBudget().subtract(weeklyCost);
        var budgetExceeded = budgetDifference != null && budgetDifference.signum() < 0;
        var budgetDeviation = source.weeklyBudget() == null
                ? null : percentage(weeklyCost.subtract(source.weeklyBudget()).abs(), source.weeklyBudget());
        var breakdown = new GeneratedMealPlanResult.ScoreBreakdown(
                legacy.calorieScore(),
                legacy.proteinScore(),
                legacy.budgetScore(),
                legacy.varietyScore(),
                legacy.repetitionScore(),
                legacy.completenessScore(),
                legacy.preparationScore(),
                purchaseScore == null ? null : purchaseScore.purchaseCostScore(),
                purchaseScore == null ? null : purchaseScore.consumedCostScore(),
                purchaseScore == null ? null : purchaseScore.purchaseBudgetScore(),
                purchaseScore == null ? null : purchaseScore.wasteCostScore(),
                purchaseScore == null ? null : purchaseScore.wastePercentageScore(),
                purchaseScore == null ? null : purchaseScore.usefulReuseScore(),
                purchaseScore == null ? null : purchaseScore.uniqueProductsScore(),
                purchaseScore == null ? null : purchaseScore.packageCountScore(),
                purchaseScore == null ? legacy.totalScore() : purchaseScore.totalScore()
        );
        var metrics = source.strategy() == GenerationStrategy.PURCHASE_AWARE_SCORING
                ? new GeneratedMealPlanResult.PurchaseMetrics(
                        purchase.totalConsumedCost(),
                        purchase.totalPurchaseCost(),
                        purchase.totalWasteCost(),
                        purchase.wastePercentage(),
                        purchase.totalPackages(),
                        purchase.uniqueProductCount(),
                        purchase.reusedProductCount(),
                        usefulReuse,
                        purchase.purchaseBudgetDifference(),
                        purchase.purchaseBudgetExceeded(),
                        purchase.purchaseBudgetDeviationPercentage(),
                        purchase.calculationComplete(),
                        purchase.warnings().stream().map(PurchaseMetricsCalculator.PurchaseWarning::message).toList(),
                        selectionReasons(purchase)
                ) : null;
        return new GeneratedMealPlanResult(
                source.persisted(), source.mealPlanId(), source.generationToken(), source.name(),
                source.supermarketCode(), source.supermarketName(), source.startDate(),
                source.numberOfDays(), source.mealsPerDay(), source.servings(), source.seed(),
                source.strategy(), source.status(), source.criteria(), List.copyOf(recalculatedDays),
                scaleNutrition(weeklyNutrition), money(weeklyCost), metrics, source.weeklyBudget(),
                budgetDifference == null ? null : money(budgetDifference), budgetExceeded,
                budgetDeviation, breakdown.totalScore(), breakdown,
                new GeneratedMealPlanResult.VarietyMetrics(
                        legacy.uniqueTemplates(), legacy.repeatedTemplates(),
                        legacy.maximumObservedRepetition(), legacy.varietyMetricScore()
                ),
                scoredMeals.stream().allMatch(MealPlanScoringService.ScoredMeal::complete)
                        && (source.strategy() != GenerationStrategy.PURCHASE_AWARE_SCORING
                        || purchase.calculationComplete()),
                source.warnings(), source.constraintsApplied(), source.constraintsNotMet(),
                source.rejectedCandidateStatistics(), source.generationMetadata(),
                source.createdAt(), now, editVersion, contentVersion,
                source.shoppingListStatus(), source.activeShoppingListId(),
                source.canUndo(), source.lastChangeSummary()
        );
    }

    private GenerateMealPlanCommand command(GeneratedMealPlanResult source) {
        var criteria = source.criteria();
        return new GenerateMealPlanCommand(
                source.supermarketCode(), source.name(), source.startDate(), source.numberOfDays(),
                source.mealsPerDay(), source.servings(), criteria.dailyCaloriesTarget(),
                criteria.dailyProteinTarget(), source.weeklyBudget(),
                criteria.allowedMealTypes().stream().map(MealType::valueOf).collect(Collectors.toSet()),
                criteria.requiredDietaryTags(), criteria.excludedAllergens(),
                criteria.excludedTemplateIds(), criteria.excludedProductIds(),
                criteria.maximumPreparationMinutes(), criteria.maximumTemplateRepetitions(),
                criteria.varietyPreference(), criteria.allowIncompleteCalculations(),
                source.strategy(), source.generationMetadata().optimizationPreset(),
                source.seed(), null, false
        );
    }

    private int usefulReuse(List<GeneratedMealPlanResult.DayResult> days, BigDecimal budget) {
        var state = purchaseCalculator.empty(budget);
        var useful = 0;
        for (var meal : days.stream().flatMap(day -> day.meals().stream()).toList()) {
            var delta = purchaseCalculator.add(
                    state,
                    meal.ingredients().stream().map(this::purchaseInput).toList(),
                    PurchaseMetricsCalculator.ConflictMode.LENIENT
            );
            useful += delta.economicallyUsefulReuse();
            state = delta.state();
        }
        return useful;
    }

    private PurchaseMetricsCalculator.IngredientInput purchaseInput(
            GeneratedMealPlanResult.IngredientSummary value
    ) {
        return new PurchaseMetricsCalculator.IngredientInput(
                value.productId(), value.productName(), value.brand(), value.categoryId(),
                value.categoryName(), value.quantity(), value.quantityUnit(),
                value.measurementType(), value.packageQuantity(), value.packageUnit(),
                value.packagePrice(), value.unitPrice(), value.available(), value.warnings()
        );
    }

    private List<String> selectionReasons(PurchaseMetricsCalculator.PurchaseCalculation value) {
        var result = new LinkedHashSet<String>();
        if (value.reusedProductCount() > 0) result.add("Reutiliza productos presentes en varias comidas");
        if (value.wastePercentage().compareTo(new BigDecimal("30")) < 0) {
            result.add("Mantiene el desperdicio estimado por debajo del 30 %");
        }
        if (value.weeklyBudget() != null && !value.purchaseBudgetExceeded()) {
            result.add("El coste estimado de compra entra en el presupuesto");
        }
        return List.copyOf(result);
    }

    private NutritionBreakdown zeroNutrition() {
        return new NutritionBreakdown(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }

    private NutritionBreakdown scaleNutrition(NutritionBreakdown value) {
        return new NutritionBreakdown(
                nutrient(value.calories()), nutrient(value.protein()), nutrient(value.carbohydrates()),
                nutrient(value.fat()), nutrient(value.fiber()), nutrient(value.sugar()),
                nutrient(value.salt())
        );
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal base) {
        return base.signum() == 0 ? BigDecimal.ZERO
                : amount.multiply(new BigDecimal("100"))
                        .divide(base, 2, RoundingMode.HALF_UP);
    }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal nutrient(BigDecimal value) { return value.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros(); }
}
