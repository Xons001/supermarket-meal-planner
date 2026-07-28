package com.sean.supermarketmealplanner.catalog.application;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.AllergenRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.CategoryRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.DietaryTagRepository;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ProductSearchRequestParser {

    public static final int DEFAULT_SIZE = 12;
    public static final int MAX_SIZE = 48;

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "name", "name",
            "currentPrice", "currentPrice",
            "unitPrice", "unitPrice",
            "lastSyncedAt", "lastSyncedAt"
    );

    private final SupermarketRepository supermarketRepository;
    private final CategoryRepository categoryRepository;
    private final DietaryTagRepository dietaryTagRepository;
    private final AllergenRepository allergenRepository;

    public ProductSearchRequestParser(
            SupermarketRepository supermarketRepository,
            CategoryRepository categoryRepository,
            DietaryTagRepository dietaryTagRepository,
            AllergenRepository allergenRepository
    ) {
        this.supermarketRepository = supermarketRepository;
        this.categoryRepository = categoryRepository;
        this.dietaryTagRepository = dietaryTagRepository;
        this.allergenRepository = allergenRepository;
    }

    public ParsedProductSearch parse(
            String supermarketCode,
            UUID categoryId,
            String query,
            Boolean available,
            BigDecimal maximumPrice,
            BigDecimal maximumCalories,
            BigDecimal minimumProtein,
            String dietaryTags,
            String excludedAllergens,
            int page,
            int size,
            String sort
    ) {
        validatePagination(page, size);
        validateNonNegative(maximumPrice, "maximumPrice");
        validateNonNegative(maximumCalories, "maximumCalories");
        validateNonNegative(minimumProtein, "minimumProtein");

        var parsedSupermarket = parseSupermarket(supermarketCode);
        validateCategory(categoryId, parsedSupermarket);
        var parsedTags = parseCodes(dietaryTags);
        var parsedAllergens = parseCodes(excludedAllergens);
        validateDietaryTags(parsedTags);
        validateAllergens(parsedAllergens);

        var normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        if (normalizedQuery != null && normalizedQuery.length() > 120) {
            throw new InvalidFilterException("query must contain at most 120 characters");
        }

        var criteria = new ProductSearchCriteria(
                parsedSupermarket,
                categoryId,
                normalizedQuery,
                available,
                maximumPrice,
                maximumCalories,
                minimumProtein,
                parsedTags,
                parsedAllergens
        );
        return new ParsedProductSearch(criteria, PageRequest.of(page, size, parseSort(sort)));
    }

    public SupermarketCode parseOptionalSupermarket(String rawCode) {
        return parseSupermarket(rawCode);
    }

    private SupermarketCode parseSupermarket(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return null;
        }
        final SupermarketCode code;
        try {
            code = SupermarketCode.valueOf(rawCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidFilterException("Invalid supermarketCode: " + rawCode);
        }
        if (supermarketRepository.findByCode(code).isEmpty()) {
            throw new InvalidFilterException("Invalid supermarketCode: " + rawCode);
        }
        return code;
    }

    private void validateCategory(UUID categoryId, SupermarketCode supermarketCode) {
        if (categoryId == null) {
            return;
        }
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new InvalidFilterException("Category not found: " + categoryId));
        if (supermarketCode != null && category.getSupermarket().getCode() != supermarketCode) {
            throw new InvalidFilterException(
                    "Category does not belong to supermarket " + supermarketCode
            );
        }
    }

    private void validateDietaryTags(Set<String> codes) {
        var found = dietaryTagRepository.findAllByCodeIn(codes).stream()
                .map(tag -> tag.getCode())
                .collect(java.util.stream.Collectors.toSet());
        var unknown = new LinkedHashSet<>(codes);
        unknown.removeAll(found);
        if (!unknown.isEmpty()) {
            throw new InvalidFilterException("Invalid dietaryTags: " + String.join(", ", unknown));
        }
    }

    private void validateAllergens(Set<String> codes) {
        var found = allergenRepository.findAllByCodeIn(codes).stream()
                .map(allergen -> allergen.getCode())
                .collect(java.util.stream.Collectors.toSet());
        var unknown = new LinkedHashSet<>(codes);
        unknown.removeAll(found);
        if (!unknown.isEmpty()) {
            throw new InvalidFilterException(
                    "Invalid excludedAllergens: " + String.join(", ", unknown)
            );
        }
    }

    private Set<String> parseCodes(String rawCodes) {
        if (rawCodes == null || rawCodes.isBlank()) {
            return Set.of();
        }
        var codes = new LinkedHashSet<String>();
        Arrays.stream(rawCodes.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .forEach(codes::add);
        return Set.copyOf(codes);
    }

    private Sort parseSort(String rawSort) {
        var parts = (rawSort == null || rawSort.isBlank() ? "name,asc" : rawSort).split(",");
        if (parts.length != 2 || !SORT_FIELDS.containsKey(parts[0])) {
            throw new InvalidFilterException(
                    "Invalid sort. Use field,direction with name, currentPrice, unitPrice or lastSyncedAt"
            );
        }
        final Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new InvalidFilterException("Invalid sort direction: " + parts[1]);
        }
        return Sort.by(
                new Sort.Order(direction, SORT_FIELDS.get(parts[0])),
                Sort.Order.asc("id")
        );
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidFilterException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidFilterException("size must be between 1 and " + MAX_SIZE);
        }
    }

    private void validateNonNegative(BigDecimal value, String fieldName) {
        if (value != null && value.signum() < 0) {
            throw new InvalidFilterException(fieldName + " must be non-negative");
        }
    }
}
