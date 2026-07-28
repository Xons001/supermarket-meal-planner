package com.sean.supermarketmealplanner.mealtemplate.application;

import jakarta.validation.constraints.NotNull;

public record MealTemplateStatusRequest(@NotNull Boolean active) {
}
