package com.sean.supermarketmealplanner.mealplan.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DuplicateMealPlanRequest(
        @NotBlank @Size(max = 180) String name,
        @NotNull LocalDate startDate
) {
}
