package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class MealPlanSearchRequestParser {

    private static final Set<String> SORT_FIELDS = Set.of(
            "name", "startDate", "overallScore", "createdAt", "totalConsumedCost"
    );
    private final SupermarketRepository supermarketRepository;

    public MealPlanSearchRequestParser(SupermarketRepository supermarketRepository) {
        this.supermarketRepository = supermarketRepository;
    }

    public MealPlanSearchCriteria parse(
            String supermarketCode,
            String status,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            BigDecimal minimumScore,
            int page,
            int size,
            String sort
    ) {
        if (page < 0) {
            throw new MealPlanValidationException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 48) {
            throw new MealPlanValidationException("size must be between 1 and 48");
        }
        if (minimumScore != null
                && (minimumScore.signum() < 0
                || minimumScore.compareTo(new BigDecimal("100")) > 0)) {
            throw new MealPlanValidationException("minimumScore must be between 0 and 100");
        }
        if (startDateFrom != null && startDateTo != null
                && startDateFrom.isAfter(startDateTo)) {
            throw new MealPlanValidationException("startDateFrom must not be after startDateTo");
        }
        var sortParts = sort == null ? new String[]{"createdAt", "desc"} : sort.split(",");
        if (sortParts.length != 2 || !SORT_FIELDS.contains(sortParts[0])) {
            throw new MealPlanValidationException("Invalid meal plan sort");
        }
        var direction = switch (sortParts[1].toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new MealPlanValidationException("Invalid sort direction");
        };
        return new MealPlanSearchCriteria(
                parseSupermarket(supermarketCode),
                parseStatus(status),
                startDateFrom,
                startDateTo,
                minimumScore,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(direction, sortParts[0]).and(Sort.by("id"))
                )
        );
    }

    private SupermarketCode parseSupermarket(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            var code = SupermarketCode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            if (supermarketRepository.findByCode(code).isEmpty()) {
                throw new MealPlanValidationException("Invalid supermarketCode: " + raw);
            }
            return code;
        } catch (IllegalArgumentException exception) {
            throw new MealPlanValidationException("Invalid supermarketCode: " + raw);
        }
    }

    private MealPlanStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MealPlanStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new MealPlanValidationException("Invalid meal plan status: " + raw);
        }
    }
}
