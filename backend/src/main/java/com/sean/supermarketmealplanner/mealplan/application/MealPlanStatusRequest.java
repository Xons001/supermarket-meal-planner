package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus;
import jakarta.validation.constraints.NotNull;

public record MealPlanStatusRequest(@NotNull MealPlanStatus status) {
}
