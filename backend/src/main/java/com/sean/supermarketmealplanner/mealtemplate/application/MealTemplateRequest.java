package com.sean.supermarketmealplanner.mealtemplate.application;

import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MealTemplateRequest(
        @NotBlank String supermarketCode,
        @NotBlank @Size(max = 180) String name,
        @NotBlank String description,
        @NotNull MealType mealType,
        @NotEmpty List<@NotBlank String> instructions,
        @Min(0) int preparationMinutes,
        @Positive int servings,
        boolean active,
        @Size(max = 1000) String imageUrl,
        @NotEmpty List<@Valid MealTemplateIngredientRequest> ingredients
) {
}
