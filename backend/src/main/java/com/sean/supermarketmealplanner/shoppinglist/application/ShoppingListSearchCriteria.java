package com.sean.supermarketmealplanner.shoppinglist.application;

import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListStatus;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import java.time.OffsetDateTime;

public record ShoppingListSearchCriteria(
        SupermarketCode supermarketCode,
        ShoppingListStatus status,
        OffsetDateTime generatedFrom,
        OffsetDateTime generatedTo,
        Boolean calculationComplete,
        Boolean budgetExceeded,
        int page,
        int size,
        String sortField,
        boolean descending
) {
}
