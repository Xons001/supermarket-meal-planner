package com.sean.supermarketmealplanner.mealtemplate.application;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.AllergenRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.DietaryTagRepository;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MealTemplateSearchRequestParser {

    private static final int MAX_SIZE = 48;
    private static final Set<String> SORT_FIELDS = Set.of(
            "name",
            "preparationMinutes",
            "caloriesPerServing",
            "proteinPerServing",
            "costPerServing",
            "updatedAt"
    );

    private final SupermarketRepository supermarketRepository;
    private final DietaryTagRepository dietaryTagRepository;
    private final AllergenRepository allergenRepository;

    public MealTemplateSearchRequestParser(
            SupermarketRepository supermarketRepository,
            DietaryTagRepository dietaryTagRepository,
            AllergenRepository allergenRepository
    ) {
        this.supermarketRepository = supermarketRepository;
        this.dietaryTagRepository = dietaryTagRepository;
        this.allergenRepository = allergenRepository;
    }

    public MealTemplateSearchCriteria parse(
            String supermarketCode,
            String mealType,
            Boolean active,
            String query,
            BigDecimal minimumProtein,
            BigDecimal maximumCalories,
            Integer maximumPreparationMinutes,
            String excludedAllergens,
            String dietaryTags,
            int page,
            int size,
            String sort
    ) {
        if (page < 0) {
            throw new InvalidMealTemplateFilterException(
                    "page must be greater than or equal to 0"
            );
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidMealTemplateFilterException("size must be between 1 and " + MAX_SIZE);
        }
        validateNonNegative(minimumProtein, "minimumProtein");
        validateNonNegative(maximumCalories, "maximumCalories");
        if (maximumPreparationMinutes != null && maximumPreparationMinutes < 0) {
            throw new InvalidMealTemplateFilterException(
                    "maximumPreparationMinutes must be non-negative"
            );
        }
        var normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        if (normalizedQuery != null && normalizedQuery.length() > 120) {
            throw new InvalidMealTemplateFilterException(
                    "query must contain at most 120 characters"
            );
        }
        var parsedAllergens = parseCodes(excludedAllergens);
        var parsedTags = parseCodes(dietaryTags);
        validateAllergens(parsedAllergens);
        validateTags(parsedTags);
        var sortParts = parseSort(sort);
        return new MealTemplateSearchCriteria(
                parseSupermarket(supermarketCode),
                parseMealType(mealType),
                active,
                normalizedQuery,
                minimumProtein,
                maximumCalories,
                maximumPreparationMinutes,
                parsedAllergens,
                parsedTags,
                page,
                size,
                sortParts[0],
                "desc".equals(sortParts[1])
        );
    }

    private SupermarketCode parseSupermarket(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            var code = SupermarketCode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            if (supermarketRepository.findByCode(code).isEmpty()) {
                throw new InvalidMealTemplateFilterException("Invalid supermarketCode: " + raw);
            }
            return code;
        } catch (IllegalArgumentException exception) {
            throw new InvalidMealTemplateFilterException("Invalid supermarketCode: " + raw);
        }
    }

    private MealType parseMealType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MealType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidMealTemplateFilterException("Invalid mealType: " + raw);
        }
    }

    private String[] parseSort(String raw) {
        var parts = (raw == null || raw.isBlank() ? "name,asc" : raw).split(",");
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])) {
            throw new InvalidMealTemplateFilterException(
                    "Invalid sort. Use name, preparationMinutes, caloriesPerServing, "
                            + "proteinPerServing, costPerServing or updatedAt"
            );
        }
        var direction = parts[1].toLowerCase(Locale.ROOT);
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new InvalidMealTemplateFilterException(
                    "Invalid sort direction: " + parts[1]
            );
        }
        return new String[]{parts[0], direction};
    }

    private Set<String> parseCodes(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        var values = new LinkedHashSet<String>();
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .forEach(values::add);
        return Set.copyOf(values);
    }

    private void validateTags(Set<String> codes) {
        var existing = dietaryTagRepository.findAllByCodeIn(codes).stream()
                .map(tag -> tag.getCode())
                .collect(java.util.stream.Collectors.toSet());
        var unknown = new LinkedHashSet<>(codes);
        unknown.removeAll(existing);
        if (!unknown.isEmpty()) {
            throw new InvalidMealTemplateFilterException(
                    "Invalid dietaryTags: " + String.join(", ", unknown)
            );
        }
    }

    private void validateAllergens(Set<String> codes) {
        var existing = allergenRepository.findAllByCodeIn(codes).stream()
                .map(allergen -> allergen.getCode())
                .collect(java.util.stream.Collectors.toSet());
        var unknown = new LinkedHashSet<>(codes);
        unknown.removeAll(existing);
        if (!unknown.isEmpty()) {
            throw new InvalidMealTemplateFilterException(
                    "Invalid excludedAllergens: " + String.join(", ", unknown)
            );
        }
    }

    private void validateNonNegative(BigDecimal value, String fieldName) {
        if (value != null && value.signum() < 0) {
            throw new InvalidMealTemplateFilterException(fieldName + " must be non-negative");
        }
    }
}
