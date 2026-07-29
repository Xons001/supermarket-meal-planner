package com.sean.supermarketmealplanner.mealplan.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanEntity;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanRepository;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateEntity;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateRepository;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
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

    public MealPlanService(
            java.util.List<MealPlanGenerationStrategy> strategies,
            MealPlanRepository repository,
            MealTemplateRepository templateRepository,
            SupermarketRepository supermarketRepository,
            ObjectMapper objectMapper,
            MealPlanSnapshotService snapshotService
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
                json(command.withSeedAndPersistence(
                        persisted.seed(),
                        persisted.generationToken(),
                        false
                )),
                json(persisted),
                templates
        );
        repository.saveAndFlush(entity);
        var enriched = snapshotService.decorate(entity, persisted);
        entity.updateEditedSnapshot(enriched, json(enriched), now);
        repository.saveAndFlush(entity);
        return enriched;
    }

    @Transactional(readOnly = true)
    public PageResponse<MealPlanSummaryResponse> findAll(MealPlanSearchCriteria criteria) {
        var page = repository.findAll((root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
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
        return updated;
    }

    @Transactional
    public void archive(UUID id) {
        changeStatus(id, MealPlanStatus.ARCHIVED);
    }

    private MealPlanEntity findEntity(UUID id) {
        return repository.findById(id).orElseThrow(() -> new MealPlanNotFoundException(id));
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
                entity.getUpdatedAt()
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

    private GeneratedMealPlanResult parse(String value) {
        try {
            return objectMapper.readValue(value, GeneratedMealPlanResult.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read meal plan snapshot", exception);
        }
    }
}
