package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.OptimizationPreset;
import com.sean.supermarketmealplanner.shared.application.purchase.PurchaseMetricsCalculator;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PurchaseAwareMealPlanScoringService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final MathContext MATH = MathContext.DECIMAL128;
    private final PurchaseAwareScoringProperties properties;

    public PurchaseAwareMealPlanScoringService(PurchaseAwareScoringProperties properties) {
        this.properties = properties;
    }

    public ScoreOutput score(
            GenerateMealPlanCommand command,
            MealPlanScoringService.ScoreOutput legacy,
            PurchaseMetricsCalculator.PurchaseCalculation purchase,
            int economicallyUsefulReuse
    ) {
        var reference = referenceCost(command);
        var purchaseCostScore = costScore(
                purchase.totalPurchaseCost(),
                reference,
                properties.getPurchaseCostScoreSlope()
        );
        var consumedCostScore = costScore(
                purchase.totalConsumedCost(),
                reference,
                properties.getConsumedCostScoreSlope()
        );
        var budgetScore = budgetScore(purchase.totalPurchaseCost(), command.weeklyBudget());
        var wasteCostScore = costScore(
                purchase.totalWasteCost(),
                reference,
                properties.getWasteCostScoreSlope()
        );
        var wastePercentageScore = cap(ONE_HUNDRED.subtract(purchase.wastePercentage()));
        var usefulReuseScore = purchase.reusedProductCount() == 0
                ? BigDecimal.ZERO
                : cap(percentage(
                        BigDecimal.valueOf(economicallyUsefulReuse),
                        BigDecimal.valueOf(purchase.reusedProductCount())
                ));
        var uniqueProductsScore = cap(ONE_HUNDRED.subtract(
                BigDecimal.valueOf(purchase.uniqueProductCount())
                        .multiply(properties.getUniqueProductPenalty())
        ));
        var packageCountScore = cap(ONE_HUNDRED.subtract(
                BigDecimal.valueOf(purchase.totalPackages())
                        .multiply(properties.getPackageCountPenalty())
        ));
        var factors = new LinkedHashMap<String, BigDecimal>();
        factors.put("calories", legacy.calorieScore());
        factors.put("protein", legacy.proteinScore());
        factors.put("purchaseCost", purchaseCostScore);
        factors.put("consumedCost", consumedCostScore);
        factors.put("budget", budgetScore);
        factors.put("wasteCost", wasteCostScore);
        factors.put("wastePercentage", wastePercentageScore);
        factors.put("usefulReuse", usefulReuseScore);
        factors.put("uniqueProducts", uniqueProductsScore);
        factors.put("packageCount", packageCountScore);
        factors.put("variety", legacy.varietyScore());
        factors.put("repetition", legacy.repetitionScore());
        factors.put("completeness", legacy.completenessScore());
        factors.put("preparation", legacy.preparationScore());
        var weights = properties.weightsFor(command.optimizationPreset());
        var total = factors.entrySet().stream()
                .map(entry -> entry.getValue().multiply(weights.get(entry.getKey()), MATH))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(ONE_HUNDRED, 12, RoundingMode.HALF_UP);
        return new ScoreOutput(
                scale(purchaseCostScore),
                scale(consumedCostScore),
                scale(budgetScore),
                scale(wasteCostScore),
                scale(wastePercentageScore),
                scale(usefulReuseScore),
                scale(uniqueProductsScore),
                scale(packageCountScore),
                scale(total),
                weights
        );
    }

    public BigDecimal partialPenalty(
            GenerateMealPlanCommand command,
            PurchaseMetricsCalculator.PurchaseCalculation purchase,
            int economicallyUsefulReuse,
            int selectedSlots
    ) {
        if (selectedSlots == 0) {
            return BigDecimal.ZERO;
        }
        var projectedMultiplier = BigDecimal.valueOf(
                (long) command.numberOfDays() * command.mealsPerDay()
        ).divide(BigDecimal.valueOf(selectedSlots), 12, RoundingMode.HALF_UP);
        var projected = new PurchaseMetricsCalculator.PurchaseCalculation(
                purchase.lines(),
                purchase.totalPackages(),
                purchase.totalConsumedCost().multiply(projectedMultiplier, MATH),
                purchase.totalPurchaseCost().multiply(projectedMultiplier, MATH),
                purchase.totalWasteCost().multiply(projectedMultiplier, MATH),
                purchase.wastePercentage(),
                purchase.weeklyBudget(),
                purchase.purchaseBudgetDifference(),
                purchase.purchaseBudgetExceeded(),
                purchase.purchaseBudgetDeviationPercentage(),
                purchase.calculationComplete(),
                purchase.uniqueProductCount(),
                purchase.reusedProductCount(),
                purchase.warnings()
        );
        var neutralLegacy = new MealPlanScoringService.ScoreOutput(
                ONE_HUNDRED, ONE_HUNDRED, ONE_HUNDRED, ONE_HUNDRED, ONE_HUNDRED,
                ONE_HUNDRED, ONE_HUNDRED, ONE_HUNDRED, 0, 0, 0, ONE_HUNDRED, Map.of()
        );
        var output = score(command, neutralLegacy, projected, economicallyUsefulReuse);
        return ONE_HUNDRED.subtract(output.totalScore()).max(BigDecimal.ZERO);
    }

    /**
     * A negative waste-cost delta is a real improvement: an added meal is consuming
     * capacity that had already been purchased, so it is allowed to reduce the beam penalty.
     */
    public BigDecimal marginalBonus(
            GenerateMealPlanCommand command,
            PurchaseMetricsCalculator.PurchaseDelta delta
    ) {
        var weights = properties.weightsFor(command.optimizationPreset());
        var reuseBonus = BigDecimal.valueOf(delta.economicallyUsefulReuse())
                .multiply(weights.get("usefulReuse"));
        var negativeWasteBonus = delta.wasteCostDelta().signum() < 0
                ? percentage(delta.wasteCostDelta().abs(), referenceCost(command))
                        .multiply(weights.get("wasteCost"))
                        .divide(ONE_HUNDRED, 12, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return reuseBonus.add(negativeWasteBonus);
    }

    public BigDecimal referenceCost(GenerateMealPlanCommand command) {
        return command.weeklyBudget() != null
                ? command.weeklyBudget()
                : properties.getReferenceCostPerMeal().multiply(BigDecimal.valueOf(
                        (long) command.numberOfDays() * command.mealsPerDay()
                ));
    }

    public Map<String, BigDecimal> weights(GenerateMealPlanCommand command) {
        return properties.weightsFor(command.optimizationPreset());
    }

    private BigDecimal costScore(BigDecimal cost, BigDecimal reference, BigDecimal slope) {
        if (reference == null || reference.signum() <= 0) {
            return ONE_HUNDRED;
        }
        return cap(ONE_HUNDRED.subtract(
                cost.multiply(slope, MATH).divide(reference, 12, RoundingMode.HALF_UP)
        ));
    }

    private BigDecimal budgetScore(BigDecimal purchaseCost, BigDecimal budget) {
        if (budget == null || purchaseCost.compareTo(budget) <= 0) {
            return ONE_HUNDRED;
        }
        return cap(ONE_HUNDRED.subtract(
                percentage(purchaseCost.subtract(budget), budget)
                        .multiply(properties.getBudgetExceededPenaltyFactor())
        ));
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal base) {
        return base.signum() == 0
                ? BigDecimal.ZERO
                : amount.multiply(ONE_HUNDRED, MATH)
                        .divide(base, 12, RoundingMode.HALF_UP);
    }

    private BigDecimal cap(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(ONE_HUNDRED);
    }

    private BigDecimal scale(BigDecimal value) {
        return cap(value).setScale(2, RoundingMode.HALF_UP);
    }

    public record ScoreOutput(
            BigDecimal purchaseCostScore,
            BigDecimal consumedCostScore,
            BigDecimal purchaseBudgetScore,
            BigDecimal wasteCostScore,
            BigDecimal wastePercentageScore,
            BigDecimal usefulReuseScore,
            BigDecimal uniqueProductsScore,
            BigDecimal packageCountScore,
            BigDecimal totalScore,
            Map<String, BigDecimal> weights
    ) {
    }
}
