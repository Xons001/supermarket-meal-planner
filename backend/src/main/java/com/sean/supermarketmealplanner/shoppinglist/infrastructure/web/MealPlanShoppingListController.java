package com.sean.supermarketmealplanner.shoppinglist.infrastructure.web;

import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListResponse;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListService;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meal-plans/{mealPlanId}/shopping-list")
@Tag(name = "Shopping lists")
public class MealPlanShoppingListController {

    private final ShoppingListService service;

    public MealPlanShoppingListController(ShoppingListService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate and persist a shopping list from a saved meal plan")
    public ShoppingListResponse create(@PathVariable UUID mealPlanId) {
        return service.create(mealPlanId);
    }

    @GetMapping
    @Operation(summary = "Get the active shopping list for a meal plan")
    public ShoppingListResponse findByMealPlan(@PathVariable UUID mealPlanId) {
        return service.findByMealPlanId(mealPlanId);
    }

    @PostMapping("/regenerate")
    @Operation(summary = "Transactionally replace the active shopping list")
    public ShoppingListResponse regenerate(@PathVariable UUID mealPlanId) {
        return service.regenerate(mealPlanId);
    }

    @PatchMapping("/status")
    @Operation(summary = "Archive or reactivate the latest shopping list")
    public ShoppingListResponse changeStatus(
            @PathVariable UUID mealPlanId,
            @Valid @RequestBody ShoppingListStatusRequest request
    ) {
        return service.changeStatus(mealPlanId, request.status());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logically archive the active shopping list")
    public void archive(@PathVariable UUID mealPlanId) {
        service.archive(mealPlanId);
    }
}
