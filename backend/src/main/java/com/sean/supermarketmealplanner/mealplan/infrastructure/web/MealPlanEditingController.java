package com.sean.supermarketmealplanner.mealplan.infrastructure.web;

import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanEditingDtos;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanEditingService;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/meal-plans/{planId}")
@Tag(name = "Meal plan editing")
public class MealPlanEditingController {
    private final MealPlanEditingService service;

    public MealPlanEditingController(MealPlanEditingService service) {
        this.service = service;
    }

    @GetMapping("/meals/{plannedMealId}/alternatives")
    @Operation(summary = "Rank deterministic alternatives for one persisted meal")
    public List<MealPlanEditingDtos.AlternativeResponse> alternatives(
            @PathVariable UUID planId,
            @PathVariable UUID plannedMealId,
            @RequestParam(defaultValue = "BEST_BALANCE")
            MealPlanEditingDtos.AlternativePriority priority,
            @RequestParam(required = false) Long seed,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int limit
    ) {
        return service.alternatives(planId, plannedMealId, priority, seed, limit);
    }

    @PostMapping("/meals/{plannedMealId}/replacement-previews")
    public MealPlanEditingDtos.EditPreviewResponse replacementPreview(
            @PathVariable UUID planId,
            @PathVariable UUID plannedMealId,
            @Valid @RequestBody MealPlanEditingDtos.ReplacementPreviewRequest request
    ) {
        return service.replacementPreview(
                planId, plannedMealId, request.mealTemplateId(),
                request.expectedEditVersion(), request.seed()
        );
    }

    @PostMapping("/meals/{plannedMealId}/replacements")
    public GeneratedMealPlanResult replace(
            @PathVariable UUID planId,
            @PathVariable UUID plannedMealId,
            @Valid @RequestBody MealPlanEditingDtos.ConfirmEditRequest request
    ) {
        return service.confirm(
                planId, plannedMealId, "MEAL_REPLACED",
                request.previewToken(), request.expectedEditVersion()
        );
    }

    @PostMapping("/meals/{plannedMealId}/regeneration-previews")
    public MealPlanEditingDtos.EditPreviewResponse mealRegenerationPreview(
            @PathVariable UUID planId,
            @PathVariable UUID plannedMealId,
            @Valid @RequestBody MealPlanEditingDtos.RegenerationPreviewRequest request
    ) {
        return service.mealRegenerationPreview(
                planId, plannedMealId, request.expectedEditVersion(), request.seed()
        );
    }

    @PostMapping("/meals/{plannedMealId}/regenerations")
    public GeneratedMealPlanResult regenerateMeal(
            @PathVariable UUID planId,
            @PathVariable UUID plannedMealId,
            @Valid @RequestBody MealPlanEditingDtos.ConfirmEditRequest request
    ) {
        return service.confirm(
                planId, plannedMealId, "MEAL_REGENERATED",
                request.previewToken(), request.expectedEditVersion()
        );
    }

    @PostMapping("/days/{dayId}/regeneration-previews")
    public MealPlanEditingDtos.EditPreviewResponse dayRegenerationPreview(
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @Valid @RequestBody MealPlanEditingDtos.RegenerationPreviewRequest request
    ) {
        return service.dayRegenerationPreview(
                planId, dayId, request.expectedEditVersion(), request.seed()
        );
    }

    @PostMapping("/days/{dayId}/regenerations")
    public GeneratedMealPlanResult regenerateDay(
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @Valid @RequestBody MealPlanEditingDtos.ConfirmEditRequest request
    ) {
        return service.confirm(
                planId, dayId, "DAY_REGENERATED",
                request.previewToken(), request.expectedEditVersion()
        );
    }

    @PatchMapping("/meals/{plannedMealId}/lock")
    public GeneratedMealPlanResult lock(
            @PathVariable UUID planId,
            @PathVariable UUID plannedMealId,
            @Valid @RequestBody MealPlanEditingDtos.LockRequest request
    ) {
        return service.setLocked(
                planId, plannedMealId, request.locked(), request.expectedEditVersion()
        );
    }

    @PostMapping("/undo")
    public GeneratedMealPlanResult undo(
            @PathVariable UUID planId,
            @Valid @RequestBody MealPlanEditingDtos.UndoRequest request
    ) {
        return service.undo(planId, request.expectedEditVersion());
    }

    @GetMapping("/changes")
    public PageResponse<MealPlanEditingDtos.ChangeResponse> changes(
            @PathVariable UUID planId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return service.changes(planId, page, size);
    }
}
