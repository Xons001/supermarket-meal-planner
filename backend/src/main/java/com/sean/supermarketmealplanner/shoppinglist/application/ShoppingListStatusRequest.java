package com.sean.supermarketmealplanner.shoppinglist.application;

import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListStatus;
import jakarta.validation.constraints.NotNull;

public record ShoppingListStatusRequest(@NotNull ShoppingListStatus status) {
}
