package com.sean.supermarketmealplanner.mealplan.infrastructure.web;

import com.sean.supermarketmealplanner.mealplan.application.GenerateMealPlanCommand;
import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanSearchRequestParser;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanService;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanStatusRequest;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanSummaryResponse;
import com.sean.supermarketmealplanner.mealplan.application.DuplicateMealPlanRequest;
import com.sean.supermarketmealplanner.mealplan.application.FavoriteMealPlanRequest;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import com.sean.supermarketmealplanner.identity.application.CurrentUserProvider;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserPreferencesRepository;
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
    private final CurrentUserProvider currentUser;
    private final UserPreferencesRepository preferences;

    public MealPlanController(
            MealPlanService service,
            MealPlanSearchRequestParser searchParser,
            CurrentUserProvider currentUser,
            UserPreferencesRepository preferences
    ) {
        this.service = service;
        this.searchParser = searchParser;
        this.currentUser = currentUser;
        this.preferences = preferences;
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Generate a deterministic meal plan, optionally persisting it",
            description = "PURCHASE_AWARE_SCORING is the default. SCORING preserves the classic "
                    + "consumed-cost algorithm and ignores optimizationPreset."
    )
    public GeneratedMealPlanResult generate(
            @Valid @RequestBody GenerateMealPlanRequest request
    ) {
        var userId = currentUser.userId();
        var pref = preferences.findById(userId).orElseThrow();
        return service.generate(new GenerateMealPlanCommand(
                request.supermarketCode(), request.name(), request.startDate(),
                request.numberOfDays() == null ? pref.getNumberOfDays() : request.numberOfDays(),
                request.mealsPerDay() == null ? pref.getMealsPerDay() : request.mealsPerDay(),
                request.servings() == null ? 1 : request.servings(),
                request.dailyCaloriesTarget() == null ? pref.getDailyCaloriesTarget() : request.dailyCaloriesTarget(),
                request.dailyProteinTarget() == null ? pref.getDailyProteinTarget() : request.dailyProteinTarget(),
                request.weeklyBudget() == null ? pref.getWeeklyBudget() : request.weeklyBudget(),
                request.allowedMealTypes(), union(pref.getDietaryRestrictions(), request.requiredDietaryTags()),
                union(pref.getAllergens(), request.excludedAllergens()), request.excludedTemplateIds(),
                request.excludedProductIds(), request.maximumPreparationMinutes(),
                request.maximumTemplateRepetitions(), request.varietyPreference(),
                Boolean.TRUE.equals(request.allowIncompleteCalculations()),
                request.strategy() == null ? pref.getStrategy() : request.strategy(),
                request.strategy() == com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy.SCORING
                        ? null : request.optimizationPreset() == null ? pref.getPreset() : request.optimizationPreset(),
                request.deterministicSeed(), request.generationToken(), Boolean.TRUE.equals(request.persist()),
                userId
        ));
    }

    private static java.util.Set<String> union(java.util.List<String> defaults, java.util.Set<String> explicit) {
        if (explicit != null) return explicit;
        return java.util.Set.copyOf(defaults);
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
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) String strategy,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(required = false) Boolean archived,
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
                query,
                strategy,
                favorite,
                archived,
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

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive a plan without changing its content version")
    public GeneratedMealPlanResult archiveExplicitly(@PathVariable UUID id) {
        return service.changeStatus(id, com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus.ARCHIVED);
    }

    @PatchMapping("/{id}/restore")
    @Operation(summary = "Restore an archived plan")
    public GeneratedMealPlanResult restore(@PathVariable UUID id) {
        return service.restore(id);
    }

    @PatchMapping("/{id}/favorite")
    @Operation(summary = "Set or clear a plan favorite")
    public MealPlanSummaryResponse favorite(@PathVariable UUID id,
            @RequestBody FavoriteMealPlanRequest request) {
        return service.favorite(id, request.favorite());
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Duplicate an exact historical plan snapshot")
    public GeneratedMealPlanResult duplicate(@PathVariable UUID id,
            @Valid @RequestBody DuplicateMealPlanRequest request) {
        return service.duplicate(id, request);
    }
}
