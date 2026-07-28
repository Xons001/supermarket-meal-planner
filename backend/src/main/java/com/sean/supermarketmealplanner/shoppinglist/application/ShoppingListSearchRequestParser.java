package com.sean.supermarketmealplanner.shoppinglist.application;

import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListStatus;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ShoppingListSearchRequestParser {

    private static final Set<String> SORT_FIELDS = Set.of(
            "generatedAt",
            "totalPurchaseCost",
            "totalWasteCost",
            "overallWastePercentage"
    );
    private final SupermarketRepository supermarketRepository;

    public ShoppingListSearchRequestParser(SupermarketRepository supermarketRepository) {
        this.supermarketRepository = supermarketRepository;
    }

    public ShoppingListSearchCriteria parse(
            String supermarketCode,
            String status,
            OffsetDateTime generatedFrom,
            OffsetDateTime generatedTo,
            Boolean calculationComplete,
            Boolean budgetExceeded,
            int page,
            int size,
            String sort
    ) {
        if (page < 0) {
            throw invalid("page must be greater than or equal to 0", "INVALID_PAGINATION");
        }
        if (size < 1 || size > 48) {
            throw invalid("size must be between 1 and 48", "INVALID_PAGINATION");
        }
        if (generatedFrom != null && generatedTo != null && generatedFrom.isAfter(generatedTo)) {
            throw invalid("generatedFrom must not be after generatedTo", "INVALID_DATE_RANGE");
        }
        var parts = sort == null ? new String[]{"generatedAt", "desc"} : sort.split(",");
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])) {
            throw invalid("Invalid shopping list sort", "INVALID_SORT");
        }
        var descending = switch (parts[1].toLowerCase(Locale.ROOT)) {
            case "asc" -> false;
            case "desc" -> true;
            default -> throw invalid("Invalid sort direction", "INVALID_SORT");
        };
        return new ShoppingListSearchCriteria(
                parseSupermarket(supermarketCode),
                parseStatus(status),
                generatedFrom,
                generatedTo,
                calculationComplete,
                budgetExceeded,
                page,
                size,
                parts[0],
                descending
        );
    }

    private SupermarketCode parseSupermarket(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            var code = SupermarketCode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            if (supermarketRepository.findByCode(code).isEmpty()) {
                throw invalid("Invalid supermarketCode: " + raw, "INVALID_SUPERMARKET");
            }
            return code;
        } catch (IllegalArgumentException exception) {
            throw invalid("Invalid supermarketCode: " + raw, "INVALID_SUPERMARKET");
        }
    }

    private ShoppingListStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ShoppingListStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid("Invalid shopping list status: " + raw, "INVALID_STATUS");
        }
    }

    private ShoppingListException invalid(String message, String code) {
        return new ShoppingListException(message, code, 400);
    }
}
