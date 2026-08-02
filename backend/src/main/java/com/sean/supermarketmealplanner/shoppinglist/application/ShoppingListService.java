package com.sean.supermarketmealplanner.shoppinglist.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.activity.application.ActivityService;
import com.sean.supermarketmealplanner.identity.application.CurrentUserProvider;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanEntity;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanRepository;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListStatus;
import com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence.ShoppingListEntity;
import com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence.ShoppingListRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingListService {

    private final ShoppingListRepository repository;
    private final MealPlanRepository mealPlanRepository;
    private final ShoppingListCalculationService calculationService;
    private final ShoppingListMapper mapper;
    private final ObjectMapper objectMapper;
    private final CurrentUserProvider currentUser;
    private final ActivityService activityService;

    public ShoppingListService(
            ShoppingListRepository repository,
            MealPlanRepository mealPlanRepository,
            ShoppingListCalculationService calculationService,
            ShoppingListMapper mapper,
            ObjectMapper objectMapper,
            CurrentUserProvider currentUser,
            ActivityService activityService
    ) {
        this.repository = repository;
        this.mealPlanRepository = mealPlanRepository;
        this.calculationService = calculationService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.currentUser = currentUser;
        this.activityService = activityService;
    }

    @Transactional
    @io.micrometer.core.annotation.Timed(value = "shopping.list.creation", histogram = true)
    public ShoppingListResponse create(UUID mealPlanId) {
        var plan = findPlan(mealPlanId);
        if (repository.existsByMealPlanIdAndActiveTrue(mealPlanId)) {
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
        return mapper.toResponse(repository.findByMealPlanIdAndOwnerIdAndActiveTrue(
                        mealPlanId, currentUser.userId())
                .orElseThrow(() -> error(
                        "Shopping list not found for meal plan " + mealPlanId,
                        "SHOPPING_LIST_NOT_FOUND",
                        404
                )));
    }

    @Transactional
    @io.micrometer.core.annotation.Timed(value = "shopping.list.regeneration", histogram = true)
    public ShoppingListResponse regenerate(UUID mealPlanId) {
        var plan = findPlan(mealPlanId);
        var current = repository.findByMealPlanIdAndOwnerIdAndActiveTrue(
                        mealPlanId, currentUser.userId())
                .orElseThrow(() -> error(
                        "Shopping list not found for meal plan " + mealPlanId,
                        "SHOPPING_LIST_NOT_FOUND",
                        404
                ));
        var started = System.nanoTime();
        var calculation = calculationService.calculate(parseSnapshot(plan));
        var now = OffsetDateTime.now();
        var duration = (System.nanoTime() - started) / 1_000_000;
        current.deactivate(now);
        repository.saveAndFlush(current);
        var replacement = new ShoppingListEntity(plan, calculation, duration, now);
        repository.saveAndFlush(replacement);
        activityService.record(plan.getOwner(), "SHOPPING_LIST_REGENERATED", "Lista de compra regenerada",
                "SHOPPING_LIST", replacement.getId(), plan.getId(),
                java.util.Map.of("previousShoppingListId", current.getId()));
        return mapper.toResponse(replacement);
    }

    @Transactional(readOnly = true)
    public ShoppingListResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ShoppingListSummaryResponse> findAll(
            ShoppingListSearchCriteria criteria
    ) {
        var property = switch (criteria.sortField()) {
            case "totalPurchaseCost" -> "totalPurchaseCost";
            case "totalWasteCost" -> "totalWasteCost";
            case "overallWastePercentage" -> "overallWastePercentage";
            default -> "generatedAt";
        };
        var direction = criteria.descending() ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(criteria.page(), criteria.size(),
                Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id")));
        var page = repository.findAll(specification(criteria), pageable);
        var content = page.getContent().stream().map(mapper::toSummary).toList();
        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast()
        );
    }

    @Transactional
    public ShoppingListResponse changeStatus(
            UUID mealPlanId,
            ShoppingListStatus requestedStatus
    ) {
        var entity = repository.findAllByOwnerId(currentUser.userId()).stream()
                .filter(value -> value.getMealPlan().getId().equals(mealPlanId))
                .max(java.util.Comparator.comparing(ShoppingListEntity::getGeneratedAt))
                .orElseThrow(() -> error(
                        "Shopping list not found for meal plan " + mealPlanId,
                        "SHOPPING_LIST_NOT_FOUND",
                        404
                ));
        if (requestedStatus == ShoppingListStatus.GENERATED
                && repository.findByMealPlanIdAndActiveTrue(mealPlanId)
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
        var entity = repository.findByMealPlanIdAndOwnerIdAndActiveTrue(
                        mealPlanId, currentUser.userId())
                .orElseThrow(() -> error(
                        "Active shopping list not found for meal plan " + mealPlanId,
                        "SHOPPING_LIST_NOT_FOUND",
                        404
                ));
        entity.changeStatus(ShoppingListStatus.ARCHIVED, OffsetDateTime.now());
        repository.save(entity);
    }

    @Transactional
    public ShoppingListResponse archiveById(UUID id) {
        var entity = findEntity(id);
        entity.changeStatus(ShoppingListStatus.ARCHIVED, OffsetDateTime.now());
        repository.saveAndFlush(entity);
        activityService.record(entity.getOwner(), "SHOPPING_LIST_ARCHIVED", "Lista de compra archivada",
                "SHOPPING_LIST", entity.getId(), entity.getMealPlan().getId(), java.util.Map.of());
        return mapper.toResponse(entity);
    }

    @Transactional
    public ShoppingListResponse restore(UUID id) {
        var entity = findEntity(id);
        if (!entity.isArchived()) {
            return mapper.toResponse(entity);
        }
        entity.changeStatus(ShoppingListStatus.GENERATED, OffsetDateTime.now());
        entity.deactivate(OffsetDateTime.now());
        repository.saveAndFlush(entity);
        activityService.record(entity.getOwner(), "SHOPPING_LIST_RESTORED", "Lista de compra restaurada",
                "SHOPPING_LIST", entity.getId(), entity.getMealPlan().getId(),
                java.util.Map.of("active", false));
        return mapper.toResponse(entity);
    }

    @Transactional
    public ShoppingListResponse activate(UUID id) {
        var entity = findEntity(id);
        if (entity.isArchived()) {
            throw error("Archived shopping lists must be restored before activation",
                    "SHOPPING_LIST_ARCHIVED", 422);
        }
        var now = OffsetDateTime.now();
        repository.findByMealPlanIdAndOwnerIdAndActiveTrue(
                        entity.getMealPlan().getId(), currentUser.userId())
                .filter(current -> !current.getId().equals(id))
                .ifPresent(current -> {
                    current.deactivate(now);
                    repository.save(current);
                });
        entity.activate(now);
        repository.saveAndFlush(entity);
        activityService.record(entity.getOwner(), "SHOPPING_LIST_ACTIVATED", "Lista de compra activada",
                "SHOPPING_LIST", entity.getId(), entity.getMealPlan().getId(), java.util.Map.of());
        return mapper.toResponse(entity);
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
        var entity = repository.saveAndFlush(new ShoppingListEntity(plan, calculation, duration, now));
        activityService.record(plan.getOwner(), "SHOPPING_LIST_CREATED", "Lista de compra creada",
                "SHOPPING_LIST", entity.getId(), plan.getId(), java.util.Map.of());
        return mapper.toResponse(entity);
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

    private Specification<ShoppingListEntity> specification(ShoppingListSearchCriteria criteria) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("owner").get("id"), currentUser.userId()));
            if (criteria.supermarketCode() != null) {
                predicates.add(cb.equal(root.get("supermarket").get("code"), criteria.supermarketCode()));
            }
            if (criteria.status() != null) predicates.add(cb.equal(root.get("status"), criteria.status()));
            if (criteria.generatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("generatedAt"), criteria.generatedFrom()));
            }
            if (criteria.generatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("generatedAt"), criteria.generatedTo()));
            }
            if (criteria.calculationComplete() != null) {
                predicates.add(cb.equal(root.get("calculationComplete"), criteria.calculationComplete()));
            }
            if (criteria.budgetExceeded() != null) {
                predicates.add(cb.equal(root.get("purchaseBudgetExceeded"), criteria.budgetExceeded()));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private MealPlanEntity findPlan(UUID id) {
        return mealPlanRepository.findByIdAndOwnerId(id, currentUser.userId()).orElseThrow(() -> error(
                "Meal plan not found: " + id,
                "MEAL_PLAN_NOT_FOUND",
                404
        ));
    }

    private ShoppingListEntity findEntity(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUser.userId()).orElseThrow(() -> error(
                "Shopping list not found: " + id,
                "SHOPPING_LIST_NOT_FOUND",
                404
        ));
    }

    private ShoppingListException error(String message, String code, int status) {
        return new ShoppingListException(message, code, status);
    }
}
