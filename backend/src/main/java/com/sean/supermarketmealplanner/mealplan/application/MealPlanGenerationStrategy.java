package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;

public interface MealPlanGenerationStrategy {

    GenerationStrategy supportedStrategy();

    GeneratedMealPlanResult generate(GenerateMealPlanCommand command);
}
