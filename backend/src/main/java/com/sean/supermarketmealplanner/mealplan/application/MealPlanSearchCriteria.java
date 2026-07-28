package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

public record MealPlanSearchCriteria(
        SupermarketCode supermarketCode,
        MealPlanStatus status,
        LocalDate startDateFrom,
        LocalDate startDateTo,
        BigDecimal minimumScore,
        Pageable pageable
) {
}
