package com.sean.supermarketmealplanner.mealplan.infrastructure.web;

import com.sean.supermarketmealplanner.mealplan.application.GenerateMealPlanCommand;
import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanSearchRequestParser;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanService;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanStatusRequest;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanSummaryResponse;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meal-plans")
@Tag(name = "Meal plans")
public class MealPlanController {

    private final MealPlanService service;
    private final MealPlanSearchRequestParser searchParser;

    public MealPlanController(
            MealPlanService service,
            MealPlanSearchRequestParser searchParser
    ) {
        this.service = service;
        this.searchParser = searchParser;
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Generate a deterministic meal plan, optionally persisting it",
            description = "PURCHASE_AWARE_SCORING is the default. SCORING preserves the classic "
                    + "consumed-cost algorithm and ignores optimizationPreset."
    )
    public GeneratedMealPlanResult generate(
            @Valid @RequestBody GenerateMealPlanCommand command
    ) {
        return service.generate(command);
    }

    @GetMapping
    @Operation(summary = "List saved meal plans")
    public PageResponse<MealPlanSummaryResponse> findAll(
            @RequestParam(required = false) String supermarketCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,
            @RequestParam(required = false) BigDecimal minimumScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return service.findAll(searchParser.parse(
                supermarketCode,
                status,
                startDateFrom,
                startDateTo,
                minimumScore,
                page,
                size,
                sort
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a saved meal plan snapshot")
    public GeneratedMealPlanResult findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Archive or reactivate a saved meal plan")
    public GeneratedMealPlanResult changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody MealPlanStatusRequest request
    ) {
        return service.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logically archive a saved meal plan")
    public void archive(@PathVariable UUID id) {
        service.archive(id);
    }
}
