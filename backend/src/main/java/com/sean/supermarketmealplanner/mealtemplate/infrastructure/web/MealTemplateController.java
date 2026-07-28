package com.sean.supermarketmealplanner.mealtemplate.infrastructure.web;

import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateRequest;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateResponse;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateSearchRequestParser;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateService;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateStatusRequest;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meal-templates")
@Tag(name = "Meal templates")
public class MealTemplateController {

    private final MealTemplateService service;
    private final MealTemplateSearchRequestParser searchParser;

    public MealTemplateController(
            MealTemplateService service,
            MealTemplateSearchRequestParser searchParser
    ) {
        this.service = service;
        this.searchParser = searchParser;
    }

    @GetMapping
    @Operation(summary = "List and filter reusable meal templates")
    public PageResponse<MealTemplateResponse> findAll(
            @RequestParam(required = false) String supermarketCode,
            @RequestParam(required = false) String mealType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BigDecimal minimumProtein,
            @RequestParam(required = false) BigDecimal maximumCalories,
            @RequestParam(required = false) Integer maximumPreparationMinutes,
            @RequestParam(required = false) String excludedAllergens,
            @RequestParam(required = false) String dietaryTags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        return service.findAll(searchParser.parse(
                supermarketCode,
                mealType,
                active,
                query,
                minimumProtein,
                maximumCalories,
                maximumPreparationMinutes,
                excludedAllergens,
                dietaryTags,
                page,
                size,
                sort
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a calculated meal template detail")
    public MealTemplateResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a meal template")
    public MealTemplateResponse create(@Valid @RequestBody MealTemplateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a meal template")
    public MealTemplateResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody MealTemplateRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a meal template")
    public MealTemplateResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody MealTemplateStatusRequest request
    ) {
        return service.changeStatus(id, request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive a meal template without deleting its history")
    public void archive(@PathVariable UUID id) {
        service.archive(id);
    }

    @PostMapping("/preview")
    @Operation(summary = "Validate and calculate a meal template without persisting it")
    public MealTemplateResponse preview(@Valid @RequestBody MealTemplateRequest request) {
        return service.preview(request);
    }
}
