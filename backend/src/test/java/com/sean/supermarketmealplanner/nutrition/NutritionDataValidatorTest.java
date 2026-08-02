package com.sean.supermarketmealplanner.nutrition;

import static org.assertj.core.api.Assertions.*;

import com.sean.supermarketmealplanner.nutrition.application.*;
import com.sean.supermarketmealplanner.nutrition.infrastructure.persistence.NutritionEntity.NutritionValues;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NutritionDataValidatorTest {
    private final NutritionDataValidator validator = new NutritionDataValidator();

    @Test void rejectsNegativeValuesWithoutInventingDefaults() {
        var values = new NutritionValues("PER_100_GRAMS", BigDecimal.TEN, new BigDecimal("-1"), null, null, null, null, null, null);
        assertThatThrownBy(() -> validator.validate(values)).isInstanceOf(NutritionException.class);
    }

    @Test void acceptsPartialValuesAndReportsSuspiciousValues() {
        var values = new NutritionValues("PER_100_GRAMS", new BigDecimal("1200"), null, null, null, null, null, null, null);
        assertThat(validator.validate(values)).containsExactly("calories supera el límite razonable por base nutricional");
    }
}
