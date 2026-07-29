package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.OptimizationPreset;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.meal-plans.purchase-aware")
public class PurchaseAwareScoringProperties {

    private BigDecimal referenceCostPerMeal = new BigDecimal("2.50");
    private BigDecimal purchaseCostScoreSlope = new BigDecimal("35");
    private BigDecimal consumedCostScoreSlope = new BigDecimal("20");
    private BigDecimal wasteCostScoreSlope = new BigDecimal("100");
    private BigDecimal uniqueProductPenalty = new BigDecimal("4");
    private BigDecimal packageCountPenalty = new BigDecimal("2");
    private BigDecimal budgetExceededPenaltyFactor = new BigDecimal("4");
    private Map<String, BigDecimal> balanced = weights(
            22, 22, 10, 2, 15, 6, 6, 3, 2, 3, 3, 3, 2, 1
    );
    private Map<String, BigDecimal> lowerPurchaseCost = weights(
            21, 21, 16, 2, 18, 5, 4, 3, 2, 3, 2, 1, 1, 1
    );
    private Map<String, BigDecimal> lowerWaste = weights(
            20, 20, 8, 2, 14, 12, 12, 4, 1, 2, 2, 1, 1, 1
    );
    private Map<String, BigDecimal> moreReuse = weights(
            21, 21, 6, 2, 14, 6, 6, 10, 5, 3, 2, 2, 1, 1
    );

    public BigDecimal getReferenceCostPerMeal() { return referenceCostPerMeal; }
    public void setReferenceCostPerMeal(BigDecimal value) { referenceCostPerMeal = value; }
    public BigDecimal getPurchaseCostScoreSlope() { return purchaseCostScoreSlope; }
    public void setPurchaseCostScoreSlope(BigDecimal value) { purchaseCostScoreSlope = value; }
    public BigDecimal getConsumedCostScoreSlope() { return consumedCostScoreSlope; }
    public void setConsumedCostScoreSlope(BigDecimal value) { consumedCostScoreSlope = value; }
    public BigDecimal getWasteCostScoreSlope() { return wasteCostScoreSlope; }
    public void setWasteCostScoreSlope(BigDecimal value) { wasteCostScoreSlope = value; }
    public BigDecimal getUniqueProductPenalty() { return uniqueProductPenalty; }
    public void setUniqueProductPenalty(BigDecimal value) { uniqueProductPenalty = value; }
    public BigDecimal getPackageCountPenalty() { return packageCountPenalty; }
    public void setPackageCountPenalty(BigDecimal value) { packageCountPenalty = value; }
    public BigDecimal getBudgetExceededPenaltyFactor() { return budgetExceededPenaltyFactor; }
    public void setBudgetExceededPenaltyFactor(BigDecimal value) {
        budgetExceededPenaltyFactor = value;
    }
    public Map<String, BigDecimal> getBalanced() { return balanced; }
    public void setBalanced(Map<String, BigDecimal> value) { balanced = Map.copyOf(value); }
    public Map<String, BigDecimal> getLowerPurchaseCost() { return lowerPurchaseCost; }
    public void setLowerPurchaseCost(Map<String, BigDecimal> value) {
        lowerPurchaseCost = Map.copyOf(value);
    }
    public Map<String, BigDecimal> getLowerWaste() { return lowerWaste; }
    public void setLowerWaste(Map<String, BigDecimal> value) { lowerWaste = Map.copyOf(value); }
    public Map<String, BigDecimal> getMoreReuse() { return moreReuse; }
    public void setMoreReuse(Map<String, BigDecimal> value) { moreReuse = Map.copyOf(value); }

    public Map<String, BigDecimal> weightsFor(OptimizationPreset preset) {
        var selected = switch (preset) {
            case BALANCED -> balanced;
            case LOWER_PURCHASE_COST -> lowerPurchaseCost;
            case LOWER_WASTE -> lowerWaste;
            case MORE_REUSE -> moreReuse;
        };
        var total = selected.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("100")) != 0) {
            throw new IllegalStateException("Purchase-aware scoring weights must add up to 100");
        }
        return Map.copyOf(selected);
    }

    private static Map<String, BigDecimal> weights(
            int calories,
            int protein,
            int purchaseCost,
            int consumedCost,
            int budget,
            int wasteCost,
            int wastePercentage,
            int usefulReuse,
            int uniqueProducts,
            int packageCount,
            int variety,
            int repetition,
            int completeness,
            int preparation
    ) {
        var result = new LinkedHashMap<String, BigDecimal>();
        result.put("calories", BigDecimal.valueOf(calories));
        result.put("protein", BigDecimal.valueOf(protein));
        result.put("purchaseCost", BigDecimal.valueOf(purchaseCost));
        result.put("consumedCost", BigDecimal.valueOf(consumedCost));
        result.put("budget", BigDecimal.valueOf(budget));
        result.put("wasteCost", BigDecimal.valueOf(wasteCost));
        result.put("wastePercentage", BigDecimal.valueOf(wastePercentage));
        result.put("usefulReuse", BigDecimal.valueOf(usefulReuse));
        result.put("uniqueProducts", BigDecimal.valueOf(uniqueProducts));
        result.put("packageCount", BigDecimal.valueOf(packageCount));
        result.put("variety", BigDecimal.valueOf(variety));
        result.put("repetition", BigDecimal.valueOf(repetition));
        result.put("completeness", BigDecimal.valueOf(completeness));
        result.put("preparation", BigDecimal.valueOf(preparation));
        return Map.copyOf(result);
    }
}
