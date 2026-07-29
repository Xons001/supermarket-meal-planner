package com.sean.supermarketmealplanner.mealplan.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus;
import com.sean.supermarketmealplanner.identity.application.CurrentUserProvider;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserAccountRepository;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanEntity;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanRepository;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateEntity;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateRepository;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import com.sean.supermarketmealplanner.activity.application.ActivityService;
import com.sean.supermarketmealplanner.mealplan.domain.MealSelectionSource;
import com.sean.supermarketmealplanner.mealplan.domain.ShoppingListFreshness;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MealPlanService {

    private final Map<com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy,
            MealPlanGenerationStrategy> strategies;
    private final MealPlanRepository repository;
    private final MealTemplateRepository templateRepository;
    private final SupermarketRepository supermarketRepository;
    private final ObjectMapper objectMapper;
    private final MealPlanSnapshotService snapshotService;
    private final CurrentUserProvider currentUser;
    private final UserAccountRepository userRepository;
    private final ActivityService activityService;

    public MealPlanService(
            java.util.List<MealPlanGenerationStrategy> strategies,
            MealPlanRepository repository,
            MealTemplateRepository templateRepository,
            SupermarketRepository supermarketRepository,
            ObjectMapper objectMapper,
            MealPlanSnapshotService snapshotService,
            CurrentUserProvider currentUser,
            UserAccountRepository userRepository,
            ActivityService activityService
    ) {
        this.strategies = strategies.stream().collect(Collectors.toUnmodifiableMap(
                MealPlanGenerationStrategy::supportedStrategy,
                Function.identity()
        ));
        this.repository = repository;
        this.templateRepository = templateRepository;
        this.supermarketRepository = supermarketRepository;
        this.objectMapper = objectMapper;
        this.snapshotService = snapshotService;
        this.currentUser = currentUser;
        this.userRepository = userRepository;
        this.activityService = activityService;
    }

    @Transactional
    public GeneratedMealPlanResult generate(GenerateMealPlanCommand command) {
        var strategy = strategies.get(command.strategy());
        if (strategy == null) {
            throw new MealPlanValidationException(
                    "Unsupported generation strategy: " + command.strategy()
            );
        }
        var preview = strategy.generate(command);
        if (!command.persist()) {
            return preview;
        }
        var now = OffsetDateTime.now();
        var persisted = copyWithPersistence(
                preview,
                UUID.randomUUID(),
                MealPlanStatus.GENERATED,
                now,
                now
        );
        var supermarket = supermarketRepository.findByCode(
                SupermarketCode.valueOf(persisted.supermarketCode())
        ).orElseThrow(() -> new MealPlanValidationException(
                "Invalid supermarketCode: " + persisted.supermarketCode()
        ));
        var templateIds = persisted.days().stream()
                .flatMap(day -> day.meals().stream())
                .map(GeneratedMealPlanResult.PlannedMealResult::templateId)
                .collect(Collectors.toSet());
        var templates = templateRepository.findAllById(templateIds).stream()
                .collect(Collectors.toMap(MealTemplateEntity::getId, Function.identity()));
        if (templates.size() != templateIds.size()) {
            throw new MealPlanValidationException(
                    "A selected template disappeared before the plan could be saved"
            );
        }
        var entity = new MealPlanEntity(
                persisted,
                supermarket,
                userRepository.getReferenceById(currentUser.userId()),
                json(command.withSeedAndPersistence(
                        persisted.seed(),
                        persisted.generationToken(),
                        false
                )),
                json(persisted),
                templates
        );
        repository.saveAndFlush(entity);
        activityService.record(entity.getOwner(), "MEAL_PLAN_CREATED", "Plan creado",
                "MEAL_PLAN", entity.getId(), null, Map.of("name", entity.getName()));
        var enriched = snapshotService.decorate(entity, persisted);
        entity.updateEditedSnapshot(enriched, json(enriched), now);
        repository.saveAndFlush(entity);
        return enriched;
    }

    @Transactional(readOnly = true)
    public PageResponse<MealPlanSummaryResponse> findAll(MealPlanSearchCriteria criteria) {
        var page = repository.findAll((root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(builder.equal(root.get("owner").get("id"), currentUser.userId()));
            if (criteria.supermarketCode() != null) {
                predicates.add(builder.equal(
                        root.get("supermarket").get("code"),
                        criteria.supermarketCode()
                ));
            }
            if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.startDateFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        root.get("startDate"),
                        criteria.startDateFrom()
                ));
            }
            if (criteria.startDateTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(
                        root.get("startDate"),
                        criteria.startDateTo()
                ));
            }
            if (criteria.minimumScore() != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        root.get("overallScore"),
                        criteria.minimumScore()
                ));
            }
            if (criteria.query() != null) {
                var pattern = "%" + criteria.query().toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.equal(root.get("id").as(String.class), criteria.query())
                ));
            }
            if (criteria.strategy() != null) {
                predicates.add(builder.equal(root.get("generationStrategy"), criteria.strategy()));
            }
            if (criteria.favorite() != null) {
                predicates.add(builder.equal(root.get("favorite"), criteria.favorite()));
            }
            if (criteria.archived() != null) {
                predicates.add(builder.equal(root.get("archived"), criteria.archived()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        }, criteria.pageable()).map(this::toSummary);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public GeneratedMealPlanResult findById(UUID id) {
        return snapshotService.read(findEntity(id));
    }

    @Transactional
    public GeneratedMealPlanResult changeStatus(UUID id, MealPlanStatus status) {
        if (status == MealPlanStatus.DRAFT) {
            throw new MealPlanValidationException("A persisted plan cannot return to DRAFT");
        }
        var entity = findEntity(id);
        var current = snapshotService.read(entity);
        var now = OffsetDateTime.now();
        var updated = copyWithPersistence(current, id, status, current.createdAt(), now);
        var json = json(updated);
        try {
            entity.changeStatus(status, json, now);
        } catch (IllegalArgumentException exception) {
            throw new MealPlanValidationException(exception.getMessage());
        }
        repository.saveAndFlush(entity);
        activityService.record(entity.getOwner(),
                status == MealPlanStatus.ARCHIVED ? "MEAL_PLAN_ARCHIVED" : "MEAL_PLAN_RESTORED",
                status == MealPlanStatus.ARCHIVED ? "Plan archivado" : "Plan restaurado",
                "MEAL_PLAN", entity.getId(), null, Map.of("name", entity.getName()));
        return updated;
    }

    @Transactional
    public void archive(UUID id) {
        changeStatus(id, MealPlanStatus.ARCHIVED);
    }

    @Transactional
    public GeneratedMealPlanResult restore(UUID id) {
        return changeStatus(id, MealPlanStatus.GENERATED);
    }

    @Transactional
    public MealPlanSummaryResponse favorite(UUID id, boolean favorite) {
        var entity = findEntity(id);
        entity.setFavorite(favorite, OffsetDateTime.now());
        repository.saveAndFlush(entity);
        activityService.record(entity.getOwner(), favorite ? "MEAL_PLAN_FAVORITED" : "MEAL_PLAN_UNFAVORITED",
                favorite ? "Plan marcado como favorito" : "Plan eliminado de favoritos",
                "MEAL_PLAN", id, null, Map.of("favorite", favorite));
        return toSummary(entity);
    }

    @Transactional
    public GeneratedMealPlanResult duplicate(UUID id, DuplicateMealPlanRequest request) {
        var sourceEntity = findEntity(id);
        var source = snapshotService.read(sourceEntity);
        var now = OffsetDateTime.now();
        var newPlanId = UUID.randomUUID();
        var shift = java.time.temporal.ChronoUnit.DAYS.between(source.startDate(), request.startDate());
        var days = source.days().stream().map(day -> {
            var meals = day.meals().stream().map(meal -> new GeneratedMealPlanResult.PlannedMealResult(
                    meal.position(), meal.mealType(), meal.templateId(), meal.templateName(), meal.servings(),
                    meal.preparationMinutes(), meal.ingredients(), meal.nutrition(), meal.consumedCost(),
                    meal.score(), meal.calculationComplete(), meal.warnings(), UUID.randomUUID(),
                    meal.locked(), MealSelectionSource.DUPLICATED, 0, now,
                    meal.originalMealTemplateId(), meal.partialGenerationSeed()
            )).toList();
            return new GeneratedMealPlanResult.DayResult(
                    day.dayIndex(), day.date().plusDays(shift), meals, day.totalNutrition(),
                    day.totalConsumedCost(), day.calorieTarget(), day.proteinTarget(), day.calorieDeviation(),
                    day.calorieDeviationPercentage(), day.proteinDeviation(), day.dailyScore(),
                    day.warnings(), UUID.randomUUID()
            );
        }).toList();
        var oldMeta = source.generationMetadata();
        var metadata = new GeneratedMealPlanResult.GenerationMetadata(
                oldMeta.strategy(), oldMeta.seed(), 0, oldMeta.candidatesEvaluated(),
                oldMeta.completePlansEvaluated(), now, oldMeta.algorithmVersion(), oldMeta.beamWidth(),
                oldMeta.candidatesPerPosition(), oldMeta.optimizationPreset(), oldMeta.scoreWeights()
        );
        var duplicated = new GeneratedMealPlanResult(
                true, newPlanId, null, request.name().trim(), source.supermarketCode(),
                source.supermarketName(), request.startDate(), source.numberOfDays(), source.mealsPerDay(),
                source.servings(), source.seed(), source.strategy(), MealPlanStatus.GENERATED,
                source.criteria(), days, source.weeklyNutrition(), source.totalConsumedCost(),
                source.purchaseMetrics(), source.weeklyBudget(), source.budgetDifference(),
                source.budgetExceeded(), source.budgetDeviationPercentage(), source.overallScore(),
                source.scoreBreakdown(), source.varietyMetrics(), source.calculationComplete(),
                source.warnings(), source.constraintsApplied(), source.constraintsNotMet(),
                source.rejectedCandidateStatistics(), metadata, now, now, 0, 0,
                ShoppingListFreshness.NONE, null, false, null
        );
        var templateIds = days.stream().flatMap(day -> day.meals().stream())
                .map(GeneratedMealPlanResult.PlannedMealResult::templateId).collect(Collectors.toSet());
        var templates = templateRepository.findAllById(templateIds).stream()
                .collect(Collectors.toMap(MealTemplateEntity::getId, Function.identity()));
        if (templates.size() != templateIds.size()) {
            throw new MealPlanValidationException("A historical template reference is no longer available");
        }
        var entity = new MealPlanEntity(duplicated, sourceEntity.getSupermarket(), sourceEntity.getOwner(),
                criteriaWithoutTokens(sourceEntity.getCriteriaJson()), json(duplicated), templates);
        entity.markDuplicatedFrom(sourceEntity);
        repository.saveAndFlush(entity);
        activityService.record(entity.getOwner(), "MEAL_PLAN_DUPLICATED", "Plan duplicado",
                "MEAL_PLAN", entity.getId(), sourceEntity.getId(),
                Map.of("name", entity.getName(), "duplicatedFromPlanId", sourceEntity.getId()));
        return snapshotService.read(entity);
    }

    private MealPlanEntity findEntity(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUser.userId())
                .orElseThrow(() -> new MealPlanNotFoundException(id));
    }

    private MealPlanSummaryResponse toSummary(MealPlanEntity entity) {
        return new MealPlanSummaryResponse(
                entity.getId(),
                entity.getName(),
                entity.getSupermarket().getCode().name(),
                entity.getSupermarket().getName(),
                entity.getStartDate(),
                entity.getNumberOfDays(),
                entity.getMealsPerDay(),
                entity.getServings(),
                entity.getDailyCaloriesTarget(),
                entity.getDailyProteinTarget(),
                entity.getTotalConsumedCost(),
                entity.getWeeklyBudget(),
                entity.getOverallScore(),
                entity.getStatus().name(),
                entity.isCalculationComplete(),
                entity.getWarnings().size(),
                entity.getDeterministicSeed(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getGenerationStrategy().name(),
                entity.isFavorite(),
                entity.isArchived(),
                entity.getArchivedAt(),
                entity.getEstimatedPurchaseCost(),
                entity.getEstimatedWasteCost(),
                entity.getEstimatedWastePercentage(),
                entity.getEstimatedPackageCount(),
                entity.getEstimatedUniqueProductCount(),
                entity.getDuplicatedFromPlan() == null ? null : entity.getDuplicatedFromPlan().getId()
        );
    }

    private GeneratedMealPlanResult copyWithPersistence(
            GeneratedMealPlanResult value,
            UUID id,
            MealPlanStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new GeneratedMealPlanResult(
                true,
                id,
                value.generationToken(),
                value.name(),
                value.supermarketCode(),
                value.supermarketName(),
                value.startDate(),
                value.numberOfDays(),
                value.mealsPerDay(),
                value.servings(),
                value.seed(),
                value.strategy(),
                status,
                value.criteria(),
                value.days(),
                value.weeklyNutrition(),
                value.totalConsumedCost(),
                value.purchaseMetrics(),
                value.weeklyBudget(),
                value.budgetDifference(),
                value.budgetExceeded(),
                value.budgetDeviationPercentage(),
                value.overallScore(),
                value.scoreBreakdown(),
                value.varietyMetrics(),
                value.calculationComplete(),
                value.warnings(),
                value.constraintsApplied(),
                value.constraintsNotMet(),
                value.rejectedCandidateStatistics(),
                value.generationMetadata(),
                createdAt,
                updatedAt,
                value.editVersion(),
                value.contentVersion(),
                value.shoppingListStatus(),
                value.activeShoppingListId(),
                value.canUndo(),
                value.lastChangeSummary()
        );
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize meal plan snapshot", exception);
        }
    }

    private String criteriaWithoutTokens(String value) {
        try {
            var tree = objectMapper.readTree(value);
            if (tree instanceof com.fasterxml.jackson.databind.node.ObjectNode object) {
                object.remove("generationToken");
                object.put("persist", false);
            }
            return objectMapper.writeValueAsString(tree);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not sanitize duplicated plan criteria", exception);
        }
    }

    private GeneratedMealPlanResult parse(String value) {
        try {
            return objectMapper.readValue(value, GeneratedMealPlanResult.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read meal plan snapshot", exception);
        }
    }
}
