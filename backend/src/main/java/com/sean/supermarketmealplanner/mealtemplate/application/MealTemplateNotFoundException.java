package com.sean.supermarketmealplanner.mealtemplate.application;

import java.util.UUID;

public class MealTemplateNotFoundException extends RuntimeException {

    public MealTemplateNotFoundException(UUID id) {
        super("Meal template not found: " + id);
    }
}
