package com.sean.supermarketmealplanner.mealtemplate.application;

import com.sean.supermarketmealplanner.mealtemplate.domain.QuantityUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record MealTemplateIngredientRequest(
        @NotNull UUID productId,
        @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
        @NotNull QuantityUnit quantityUnit,
        boolean optional,
        @Min(0) int sortOrder,
        @Size(max = 500) String notes
) {
}
