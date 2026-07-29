package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import org.springframework.stereotype.Service;

@Service
public class PurchaseAwareScoringMealPlanGenerationStrategy
        implements MealPlanGenerationStrategy {

    private final ScoringMealPlanGenerationStrategy engine;

    public PurchaseAwareScoringMealPlanGenerationStrategy(
            ScoringMealPlanGenerationStrategy engine
    ) {
        this.engine = engine;
    }

    @Override
    public GenerationStrategy supportedStrategy() {
        return GenerationStrategy.PURCHASE_AWARE_SCORING;
    }

    @Override
    public GeneratedMealPlanResult generate(GenerateMealPlanCommand command) {
        return engine.generatePurchaseAware(command);
    }
}
