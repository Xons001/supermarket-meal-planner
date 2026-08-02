package com.sean.supermarketmealplanner.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.sean.supermarketmealplanner.nutrition.application.NutritionNameNormalizer;
import org.junit.jupiter.api.Test;

class NutritionNameNormalizerTest {
    private final NutritionNameNormalizer normalizer = new NutritionNameNormalizer();

    @Test void normalizesAccentsPunctuationAndPackageSizeDeterministically() {
        assertThat(normalizer.normalize("Leche sin lactosa, botella 1 L"))
                .isEqualTo("leche sin lactosa botella");
    }

    @Test void preservesProtectedMeaningDuringMatching() {
        assertThat(normalizer.keepsProtectedMeaning("Pan integral sin gluten", "Pan integral"))
                .isFalse();
        assertThat(normalizer.keepsProtectedMeaning("Pan integral sin gluten", "Pan integral sin gluten"))
                .isTrue();
    }
}
