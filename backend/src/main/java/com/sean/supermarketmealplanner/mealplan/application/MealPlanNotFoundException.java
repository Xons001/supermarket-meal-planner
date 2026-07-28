package com.sean.supermarketmealplanner.mealplan.application;

import java.util.UUID;

public class MealPlanNotFoundException extends RuntimeException {

    public MealPlanNotFoundException(UUID id) {
        super("Meal plan not found: " + id);
    }
}
