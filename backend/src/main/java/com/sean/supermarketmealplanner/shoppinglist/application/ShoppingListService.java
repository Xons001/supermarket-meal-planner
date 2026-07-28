package com.sean.supermarketmealplanner.shoppinglist.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanEntity;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanRepository;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListStatus;
import com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence.ShoppingListEntity;
import com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence.ShoppingListRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingListService {

    private final ShoppingListRepository repository;
    private final MealPlanRepository mealPlanRepository;
    private final ShoppingListCalculationService calculationService;
    private final ShoppingListMapper mapper;
    private final ObjectMapper objectMapper;

    public ShoppingListService(
            ShoppingListRepository repository,
            MealPlanRepository mealPlanRepository,
            ShoppingListCalculationService calculationService,
            ShoppingListMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.mealPlanRepository = mealPlanRepository;
        this.calculationService = calculationService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ShoppingListResponse create(UUID mealPlanId) {
        var plan = findPlan(mealPlanId);
        if (repository.existsByMealPlanIdAndArchivedFalse(mealPlanId)) {
            throw error(
                    "The meal plan already has an active shopping list",
                    "SHOPPING_LIST_ALREADY_EXISTS",
                    409
            );
        }
        return generateAndSave(plan);
    }

    @Transactional(readOnly = true)
    public ShoppingListResponse findByMealPlanId(UUID mealPlanId) {
        findPlan(mealPlanId);
        return mapper.toResponse(repository.findByMealPlanIdAndArchivedFalse(mealPlanId)
                .orElseThrow(() -> error(
                        "Shopping list not found for meal plan " + mealPlanId,
                        "SHOPPING_LIST_NOT_FOUND",
                        404
                )));
    }

    @Transactional
    public ShoppingListResponse regenerate(UUID mealPlanId) {
        var plan = findPlan(mealPlanId);
        var current = repository.findByMealPlanIdAndArchivedFalse(mealPlanId)
                .orElseThrow(() -> error(
                        "Shopping list not found for meal plan " + mealPlanId,
                        "SHOPPING_LIST_NOT_FOUND",
                        404
                ));
        var started = System.nanoTime();
        var calculation = calculationService.calculate(parseSnapshot(plan));
        var now = OffsetDateTime.now();
        var duration = (System.nanoTime() - started) / 1_000_000;
        current.changeStatus(ShoppingListStatus.ARCHIVED, now);
        repository.saveAndFlush(current);
        var replacement = new ShoppingListEntity(plan, calculation, duration, now);
        return mapper.toResponse(repository.saveAndFlush(replacement));
    }

    @Transactional(readOnly = true)
    public ShoppingListResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ShoppingListSummaryResponse> findAll(
            ShoppingListSearchCriteria criteria
    ) {
        var values = repository.findAll().stream()
                .filter(value -> matches(value, criteria))
                .sorted(comparator(criteria))
                .toList();
        var total = values.size();
        var start = Math.min(criteria.page() * criteria.size(), total);
        var end = Math.min(start + criteria.size(), total);
        var content = values.subList(start, end).stream().map(mapper::toSummary).toList();
        var pages = total == 0 ? 0 : (int) Math.ceil((double) total / criteria.size());
        return new PageResponse<>(
                content,
                criteria.page(),
                criteria.size(),
                total,
                pages,
                criteria.page() == 0,
                criteria.page() + 1 >= pages
        );
    }

    @Transactional
    public ShoppingListResponse changeStatus(
            UUID mealPlanId,
            ShoppingListStatus requestedStatus
    ) {
        var entity = repository.findAll().stream()
                .filter(value -> value.getMealPlan().getId().equals(mealPlanId))
                .max(Comparator.comparing(ShoppingListEntity::getGeneratedAt))
                .orElseThrow(() -> error(
                        "Shopping list not found for meal plan " + mealPlanId,
                        "SHOPPING_LIST_NOT_FOUND",
                        404
                ));
        if (requestedStatus == ShoppingListStatus.GENERATED
                && repository.findByMealPlanIdAndArchivedFalse(mealPlanId)
                        .filter(active -> !active.getId().equals(entity.getId()))
                        .isPresent()) {
            throw error(
                    "The meal plan already has another active shopping list",
                    "SHOPPING_LIST_ALREADY_EXISTS",
                    409
            );
        }
        entity.changeStatus(requestedStatus, OffsetDateTime.now());
        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public void archive(UUID mealPlanId) {
        var entity = repository.findByMealPlanIdAndArchivedFalse(mealPlanId)
                .orElseThrow(() -> error(
                        "Active shopping list not found for meal plan " + mealPlanId,
                        "SHOPPING_LIST_NOT_FOUND",
                        404
                ));
        entity.changeStatus(ShoppingListStatus.ARCHIVED, OffsetDateTime.now());
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID id, ShoppingListCsvExporter exporter) {
        return exporter.export(mapper.toResponse(findEntity(id)));
    }

    private ShoppingListResponse generateAndSave(MealPlanEntity plan) {
        var started = System.nanoTime();
        var calculation = calculationService.calculate(parseSnapshot(plan));
        var now = OffsetDateTime.now();
        var duration = (System.nanoTime() - started) / 1_000_000;
        return mapper.toResponse(repository.saveAndFlush(
                new ShoppingListEntity(plan, calculation, duration, now)
        ));
    }

    private GeneratedMealPlanResult parseSnapshot(MealPlanEntity plan) {
        try {
            return objectMapper.readValue(plan.getResultJson(), GeneratedMealPlanResult.class);
        } catch (JsonProcessingException exception) {
            throw error(
                    "The meal plan snapshot cannot be read",
                    "MEAL_PLAN_SNAPSHOT_INSUFFICIENT",
                    422
            );
        }
    }

    private boolean matches(
            ShoppingListEntity value,
            ShoppingListSearchCriteria criteria
    ) {
        if (criteria.supermarketCode() != null
                && value.getSupermarket().getCode() != criteria.supermarketCode()) {
            return false;
        }
        if (criteria.status() != null && value.getStatus() != criteria.status()) {
            return false;
        }
        if (criteria.generatedFrom() != null
                && value.getGeneratedAt().isBefore(criteria.generatedFrom())) {
            return false;
        }
        if (criteria.generatedTo() != null
                && value.getGeneratedAt().isAfter(criteria.generatedTo())) {
            return false;
        }
        if (criteria.calculationComplete() != null
                && value.isCalculationComplete() != criteria.calculationComplete()) {
            return false;
        }
        return criteria.budgetExceeded() == null
                || value.isPurchaseBudgetExceeded() == criteria.budgetExceeded();
    }

    private Comparator<ShoppingListEntity> comparator(ShoppingListSearchCriteria criteria) {
        Comparator<ShoppingListEntity> result = switch (criteria.sortField()) {
            case "totalPurchaseCost" -> Comparator.comparing(
                    ShoppingListEntity::getTotalPurchaseCost
            );
            case "totalWasteCost" -> Comparator.comparing(
                    ShoppingListEntity::getTotalWasteCost
            );
            case "overallWastePercentage" -> Comparator.comparing(
                    ShoppingListEntity::getOverallWastePercentage
            );
            default -> Comparator.comparing(ShoppingListEntity::getGeneratedAt);
        };
        if (criteria.descending()) {
            result = result.reversed();
        }
        return result.thenComparing(ShoppingListEntity::getId);
    }

    private MealPlanEntity findPlan(UUID id) {
        return mealPlanRepository.findById(id).orElseThrow(() -> error(
                "Meal plan not found: " + id,
                "MEAL_PLAN_NOT_FOUND",
                404
        ));
    }

    private ShoppingListEntity findEntity(UUID id) {
        return repository.findById(id).orElseThrow(() -> error(
                "Shopping list not found: " + id,
                "SHOPPING_LIST_NOT_FOUND",
                404
        ));
    }

    private ShoppingListException error(String message, String code, int status) {
        return new ShoppingListException(message, code, status);
    }
}
