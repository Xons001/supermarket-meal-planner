package com.sean.supermarketmealplanner.mealplan.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanChangeType;
import com.sean.supermarketmealplanner.mealplan.domain.MealSelectionSource;
import com.sean.supermarketmealplanner.mealplan.domain.ShoppingListFreshness;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanChangeRepository;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanEntity;
import com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence.ShoppingListRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MealPlanSnapshotService {
    private static final Set<MealPlanChangeType> CONTENT_CHANGES = Set.of(
            MealPlanChangeType.MEAL_REPLACED,
            MealPlanChangeType.MEAL_REGENERATED,
            MealPlanChangeType.DAY_REGENERATED
    );
    private final ObjectMapper objectMapper;
    private final ShoppingListRepository shoppingListRepository;
    private final MealPlanChangeRepository changeRepository;

    public MealPlanSnapshotService(
            ObjectMapper objectMapper,
            ShoppingListRepository shoppingListRepository,
            MealPlanChangeRepository changeRepository
    ) {
        this.objectMapper = objectMapper;
        this.shoppingListRepository = shoppingListRepository;
        this.changeRepository = changeRepository;
    }

    public GeneratedMealPlanResult read(MealPlanEntity entity) {
        try {
            return decorate(entity, objectMapper.readValue(
                    entity.getResultJson(),
                    GeneratedMealPlanResult.class
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read meal plan snapshot", exception);
        }
    }

    public GeneratedMealPlanResult decorate(
            MealPlanEntity entity,
            GeneratedMealPlanResult source
    ) {
        var persistedDays = entity.getDays().stream().collect(Collectors.toMap(
                value -> value.getDayIndex(),
                Function.identity()
        ));
        var days = new ArrayList<GeneratedMealPlanResult.DayResult>();
        for (var day : source.days()) {
            var persistedDay = persistedDays.get(day.dayIndex());
            if (persistedDay == null) {
                days.add(day);
                continue;
            }
            var persistedMeals = persistedDay.getMeals().stream().collect(Collectors.toMap(
                    value -> value.getPosition(),
                    Function.identity()
            ));
            var meals = day.meals().stream().map(meal -> {
                var persisted = persistedMeals.get(meal.position());
                if (persisted == null) return meal;
                return new GeneratedMealPlanResult.PlannedMealResult(
                        meal.position(), meal.mealType(), meal.templateId(), meal.templateName(),
                        meal.servings(), meal.preparationMinutes(), meal.ingredients(),
                        meal.nutrition(), meal.consumedCost(), meal.score(),
                        meal.calculationComplete(), meal.warnings(), persisted.getId(),
                        persisted.isLocked(),
                        persisted.getSelectionSource() == null
                                ? MealSelectionSource.GENERATED : persisted.getSelectionSource(),
                        persisted.getEditVersion(), persisted.getModifiedAt(),
                        persisted.getOriginalMealTemplateId(), persisted.getPartialGenerationSeed()
                );
            }).toList();
            days.add(new GeneratedMealPlanResult.DayResult(
                    day.dayIndex(), day.date(), meals, day.totalNutrition(),
                    day.totalConsumedCost(), day.calorieTarget(), day.proteinTarget(),
                    day.calorieDeviation(), day.calorieDeviationPercentage(),
                    day.proteinDeviation(), day.dailyScore(), day.warnings(), persistedDay.getId()
            ));
        }
        var activeList = shoppingListRepository.findByMealPlanIdAndArchivedFalse(entity.getId());
        var freshness = activeList
                .map(value -> value.isCurrentForPlan()
                        ? ShoppingListFreshness.CURRENT : ShoppingListFreshness.OUTDATED)
                .orElse(ShoppingListFreshness.NONE);
        var undoable = changeRepository
                .findFirstByMealPlanIdAndChangeTypeInAndUndoneByChangeIdIsNullOrderBySequenceNumberDesc(
                        entity.getId(), CONTENT_CHANGES
                );
        var latest = changeRepository.findByMealPlanIdOrderBySequenceNumberDesc(entity.getId())
                .stream().findFirst();
        var summary = latest.map(value -> new GeneratedMealPlanResult.ChangeSummary(
                value.getId(), value.getChangeType(), value.getReason(),
                value.getCreatedAt(), value.getEditVersionAfter()
        )).orElse(null);
        return new GeneratedMealPlanResult(
                source.persisted(), source.mealPlanId(), source.generationToken(), source.name(),
                source.supermarketCode(), source.supermarketName(), source.startDate(),
                source.numberOfDays(), source.mealsPerDay(), source.servings(), source.seed(),
                source.strategy(), source.status(), source.criteria(), List.copyOf(days),
                source.weeklyNutrition(), source.totalConsumedCost(), source.purchaseMetrics(),
                source.weeklyBudget(), source.budgetDifference(), source.budgetExceeded(),
                source.budgetDeviationPercentage(), source.overallScore(), source.scoreBreakdown(),
                source.varietyMetrics(), source.calculationComplete(), source.warnings(),
                source.constraintsApplied(), source.constraintsNotMet(),
                source.rejectedCandidateStatistics(), source.generationMetadata(),
                source.createdAt(), source.updatedAt(), entity.getEditVersion(),
                entity.getContentVersion(), freshness,
                activeList.map(value -> value.getId()).orElse(null),
                undoable.isPresent(), summary
        );
    }
}
