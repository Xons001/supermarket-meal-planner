package com.sean.supermarketmealplanner.mealplan.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanEditingDtos.AlternativePriority;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanEditingDtos.AlternativeResponse;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanEditingDtos.ChangeResponse;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanEditingDtos.EditPreviewResponse;
import com.sean.supermarketmealplanner.mealplan.application.MealPlanEditingDtos.MetricsSnapshot;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanChangeType;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus;
import com.sean.supermarketmealplanner.mealplan.domain.MealSelectionSource;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanChangeEntity;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanChangeRepository;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanDayEntity;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanEntity;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanRepository;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.PlannedMealEntity;
import com.sean.supermarketmealplanner.identity.application.CurrentUserProvider;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateIngredientRequest;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateRequest;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateResponse;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateSearchCriteria;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateService;
import com.sean.supermarketmealplanner.mealtemplate.application.NutritionBreakdown;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import com.sean.supermarketmealplanner.mealtemplate.domain.QuantityUnit;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateEntity;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateRepository;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import jakarta.persistence.OptimisticLockException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MealPlanEditingService {
    private static final String EDIT_ALGORITHM = "partial-edit-beam-v1";
    private static final int DAY_BEAM_WIDTH = 24;
    private static final int CANDIDATES_PER_SLOT = 8;
    private static final Set<MealPlanChangeType> CONTENT_CHANGE_TYPES = Set.of(
            MealPlanChangeType.MEAL_REPLACED,
            MealPlanChangeType.MEAL_REGENERATED,
            MealPlanChangeType.DAY_REGENERATED
    );

    private final MealPlanRepository planRepository;
    private final CurrentUserProvider currentUser;
    private final MealPlanChangeRepository changeRepository;
    private final MealTemplateRepository templateRepository;
    private final MealTemplateService templateService;
    private final ProductRepository productRepository;
    private final MealPlanSnapshotService snapshotService;
    private final MealPlanRecalculationService recalculationService;
    private final EditPreviewTokenService tokenService;
    private final ObjectMapper objectMapper;

    public MealPlanEditingService(
            MealPlanRepository planRepository,
            MealPlanChangeRepository changeRepository,
            MealTemplateRepository templateRepository,
            MealTemplateService templateService,
            ProductRepository productRepository,
            MealPlanSnapshotService snapshotService,
            MealPlanRecalculationService recalculationService,
            EditPreviewTokenService tokenService,
            ObjectMapper objectMapper,
            CurrentUserProvider currentUser
    ) {
        this.planRepository = planRepository;
        this.changeRepository = changeRepository;
        this.templateRepository = templateRepository;
        this.templateService = templateService;
        this.productRepository = productRepository;
        this.snapshotService = snapshotService;
        this.recalculationService = recalculationService;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<AlternativeResponse> alternatives(
            UUID planId,
            UUID plannedMealId,
            AlternativePriority priority,
            Long requestedSeed,
            int limit
    ) {
        var context = context(planId, plannedMealId);
        ensureEditable(context.plan());
        ensureUnlocked(context.meal());
        var seed = requestedSeed == null
                ? derivedSeed(planId, context.plan().getEditVersion(), plannedMealId)
                : requestedSeed;
        var alternatives = candidateMeals(context, seed).stream()
                .map(candidate -> evaluatedAlternative(context, candidate, seed))
                .sorted(alternativeComparator(priority).thenComparing(AlternativeResponse::mealTemplateId))
                .limit(Math.max(1, Math.min(limit, 30)))
                .toList();
        if (alternatives.isEmpty()) throw noAlternative();
        var ranked = new ArrayList<AlternativeResponse>();
        for (int index = 0; index < alternatives.size(); index++) {
            var value = alternatives.get(index);
            ranked.add(new AlternativeResponse(
                    index + 1, value.mealTemplateId(), value.name(), value.mainIngredients(),
                    value.calories(), value.protein(), value.consumedCost(),
                    value.marginalPurchaseCost(), value.purchaseCostDelta(),
                    value.wasteCostDelta(), value.packageDelta(), value.uniqueProductDelta(),
                    value.varietyDelta(), value.repetitionDelta(), value.estimatedScore(),
                    value.reasons(), value.warnings(), value.seed()
            ));
        }
        return List.copyOf(ranked);
    }

    @Transactional(readOnly = true)
    public EditPreviewResponse replacementPreview(
            UUID planId,
            UUID plannedMealId,
            UUID templateId,
            long expectedVersion,
            Long requestedSeed
    ) {
        var context = context(planId, plannedMealId);
        validateVersion(context.plan(), expectedVersion);
        ensureEditable(context.plan());
        ensureUnlocked(context.meal());
        var seed = requestedSeed == null
                ? derivedSeed(planId, expectedVersion, plannedMealId) : requestedSeed;
        var candidate = candidateMeals(context, seed).stream()
                .filter(value -> value.templateId().equals(templateId))
                .findFirst().orElseThrow(this::noAlternative);
        return preview(context, List.of(candidate), "MEAL_REPLACED", seed);
    }

    @Transactional(readOnly = true)
    public EditPreviewResponse mealRegenerationPreview(
            UUID planId,
            UUID plannedMealId,
            long expectedVersion,
            Long requestedSeed
    ) {
        var context = context(planId, plannedMealId);
        validateVersion(context.plan(), expectedVersion);
        ensureEditable(context.plan());
        ensureUnlocked(context.meal());
        var seed = requestedSeed == null
                ? derivedSeed(planId, expectedVersion, plannedMealId) : requestedSeed;
        var candidate = candidateMeals(context, seed).stream()
                .map(value -> Map.entry(value, evaluatedAlternative(context, value, seed)))
                .sorted(Map.Entry.<GeneratedMealPlanResult.PlannedMealResult, AlternativeResponse>
                        comparingByValue(alternativeComparator(AlternativePriority.BEST_BALANCE)))
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow(this::noAlternative);
        return preview(context, List.of(candidate), "MEAL_REGENERATED", seed);
    }

    @Transactional(readOnly = true)
    public EditPreviewResponse dayRegenerationPreview(
            UUID planId,
            UUID dayId,
            long expectedVersion,
            Long requestedSeed
    ) {
        var plan = findPlan(planId);
        validateVersion(plan, expectedVersion);
        ensureEditable(plan);
        var day = findDay(plan, dayId);
        var editable = day.getMeals().stream().filter(meal -> !meal.isLocked()).toList();
        if (editable.isEmpty()) {
            throw new MealPlanEditingException(
                    "The day has no unlocked meals",
                    "EDIT_RULE_VIOLATION",
                    422
            );
        }
        var seed = requestedSeed == null ? derivedSeed(planId, expectedVersion, dayId) : requestedSeed;
        var source = snapshotService.read(plan);
        var beam = List.of(new DayState(List.of(), source));
        for (var meal : editable) {
            var context = context(plan, meal, source);
            var candidates = candidateMeals(context, seed).stream()
                    .limit(CANDIDATES_PER_SLOT).toList();
            if (candidates.isEmpty()) throw noAlternative();
            var expanded = new ArrayList<DayState>();
            for (var state : beam) {
                for (var candidate : candidates) {
                    var replacements = new ArrayList<>(state.replacements());
                    replacements.add(candidate);
                    var days = replaceMeals(source.days(), replacements);
                    var evaluated = recalculationService.recalculate(
                            source, days, source.editVersion(), source.contentVersion(),
                            source.updatedAt()
                    );
                    expanded.add(new DayState(List.copyOf(replacements), evaluated));
                }
            }
            beam = expanded.stream()
                    .sorted(Comparator.comparing(
                            (DayState value) -> value.result().overallScore()
                    ).reversed().thenComparing(value -> replacementKey(value.replacements())))
                    .limit(DAY_BEAM_WIDTH).toList();
        }
        var best = beam.getFirst();
        return preview(
                new EditContext(plan, day, null, source, null),
                best.replacements(),
                "DAY_REGENERATED",
                seed
        );
    }

    @Transactional
    public GeneratedMealPlanResult confirm(
            UUID planId,
            UUID targetId,
            String expectedOperation,
            String previewToken,
            long expectedVersion
    ) {
        try {
            var payload = tokenService.verify(previewToken);
            if (!payload.planId().equals(planId)
                    || !Objects.equals(payload.ownerId(), currentUser.userId())
                    || !payload.targetId().equals(targetId)
                    || !payload.operation().equals(expectedOperation)) {
                throw stale("The preview token belongs to another operation");
            }
            var plan = findPlan(planId);
            validateVersion(plan, expectedVersion);
            if (payload.editVersion() != plan.getEditVersion()) {
                throw stale("The plan changed after the preview");
            }
            var current = snapshotService.read(plan);
            var currentPreset = current.generationMetadata().optimizationPreset() == null
                    ? null : current.generationMetadata().optimizationPreset().name();
            if (!payload.strategy().equals(current.strategy().name())
                    || !Objects.equals(payload.preset(), currentPreset)
                    || !payload.algorithm().equals(current.generationMetadata().algorithmVersion())
                    || !payload.snapshotHash().equals(hash(current.days()))) {
                throw stale("The preview no longer matches current snapshots");
            }
            EditPreviewResponse refreshed;
            if ("DAY_REGENERATED".equals(expectedOperation)) {
                refreshed = dayRegenerationPreview(planId, targetId, expectedVersion, payload.seed());
            } else {
                var templateId = payload.templateIds().getFirst();
                refreshed = replacementPreview(
                        planId, targetId, templateId, expectedVersion, payload.seed()
                );
                if ("MEAL_REGENERATED".equals(expectedOperation)) {
                    refreshed = refreshed.withOperation("MEAL_REGENERATED");
                }
            }
            if (!hash(refreshed.afterMeals()).equals(payload.resultHash())) {
                throw stale("The preview no longer matches current snapshots");
            }
            if (!hash(refreshed.after()).equals(hash(payload.metrics()))) {
                throw stale("The preview metrics no longer match current snapshots");
            }
            return applyContentEdit(plan, refreshed, expectedOperation);
        } catch (OptimisticLockException | OptimisticLockingFailureException exception) {
            throw new MealPlanEditingException(
                    "The plan was modified concurrently",
                    "EDIT_CONCURRENT_MODIFICATION",
                    409
            );
        }
    }

    @Transactional
    public GeneratedMealPlanResult setLocked(
            UUID planId,
            UUID plannedMealId,
            boolean locked,
            long expectedVersion
    ) {
        var context = context(planId, plannedMealId);
        validateVersion(context.plan(), expectedVersion);
        ensureEditable(context.plan());
        if (context.meal().isLocked() == locked) {
            throw new MealPlanEditingException(
                    "The meal already has the requested lock state",
                    "EDIT_RULE_VIOLATION",
                    422
            );
        }
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var beforePlan = context.source();
        var beforeVersion = context.plan().nextVersion(false);
        context.meal().setLocked(locked, context.plan().getEditVersion(), now);
        var afterPlan = snapshotService.decorate(context.plan(), beforePlan);
        afterPlan = copyVersions(afterPlan, context.plan(), now);
        context.plan().updateEditedSnapshot(afterPlan, json(afterPlan), now);
        var type = locked ? MealPlanChangeType.MEAL_LOCKED : MealPlanChangeType.MEAL_UNLOCKED;
        var event = change(
                context.plan(), type, beforeVersion, context.day().getId(), plannedMealId,
                List.of(context.current()), List.of(findMeal(afterPlan, plannedMealId)),
                beforePlan, afterPlan, null, locked ? "Comida bloqueada" : "Comida desbloqueada",
                now
        );
        changeRepository.save(event);
        planRepository.saveAndFlush(context.plan());
        return snapshotService.decorate(context.plan(), afterPlan);
    }

    @Transactional
    public GeneratedMealPlanResult undo(UUID planId, long expectedVersion) {
        var plan = findPlan(planId);
        validateVersion(plan, expectedVersion);
        ensureEditable(plan);
        var original = changeRepository
                .findFirstByMealPlanIdAndChangeTypeInAndUndoneByChangeIdIsNullOrderBySequenceNumberDesc(
                        planId, CONTENT_CHANGE_TYPES
                ).orElseThrow(() -> new MealPlanEditingException(
                        "There is no content change available to undo",
                        "EDIT_RULE_VIOLATION",
                        422
                ));
        var source = snapshotService.read(plan);
        var beforeMeals = meals(original.getBeforeSnapshot());
        var restored = beforeMeals.stream().map(old -> {
            var current = findMeal(source, old.plannedMealId());
            return new GeneratedMealPlanResult.PlannedMealResult(
                    old.position(), old.mealType(), old.templateId(), old.templateName(),
                    old.servings(), old.preparationMinutes(), old.ingredients(), old.nutrition(),
                    old.consumedCost(), old.score(), old.calculationComplete(), old.warnings(),
                    current.plannedMealId(), current.locked(), old.selectionSource(),
                    current.editVersion(), OffsetDateTime.now(ZoneOffset.UTC),
                    old.originalMealTemplateId(), old.partialGenerationSeed()
            );
        }).toList();
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var beforeVersion = plan.nextVersion(true);
        var days = replaceMeals(source.days(), restored);
        var recalculated = recalculationService.recalculate(
                source, days, plan.getEditVersion(), plan.getContentVersion(), now
        );
        applyMeals(plan, restored, plan.getEditVersion(), null, now, true);
        updateDayTotals(plan, recalculated);
        var finalResult = snapshotService.decorate(plan, recalculated);
        finalResult = copyVersions(finalResult, plan, now);
        plan.updateEditedSnapshot(finalResult, json(finalResult), now);
        var undoEvent = change(
                plan, MealPlanChangeType.CHANGE_UNDONE, beforeVersion,
                original.getMealPlanDayId(), original.getPlannedMealId(),
                affectedCurrent(source, restored), restored, source, finalResult,
                original.getDeterministicSeed(), "Último cambio de contenido deshecho", now
        );
        changeRepository.save(undoEvent);
        original.markUndone(undoEvent.getId());
        changeRepository.save(original);
        planRepository.saveAndFlush(plan);
        return snapshotService.decorate(plan, finalResult);
    }

    @Transactional(readOnly = true)
    public PageResponse<ChangeResponse> changes(UUID planId, int page, int size) {
        findPlan(planId);
        var all = changeRepository.findByMealPlanIdOrderBySequenceNumberDesc(planId);
        var safeSize = Math.max(1, Math.min(size, 100));
        var start = Math.min(Math.max(0, page) * safeSize, all.size());
        var end = Math.min(start + safeSize, all.size());
        var content = all.subList(start, end).stream().map(this::changeResponse).toList();
        var pages = all.isEmpty() ? 0 : (int) Math.ceil((double) all.size() / safeSize);
        return new PageResponse<>(
                content, Math.max(0, page), safeSize, all.size(), pages,
                page == 0, page + 1 >= pages
        );
    }

    private GeneratedMealPlanResult applyContentEdit(
            MealPlanEntity plan,
            EditPreviewResponse preview,
            String operation
    ) {
        ensureEditable(plan);
        var source = snapshotService.read(plan);
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var beforeVersion = plan.nextVersion(true);
        var selectionSource = switch (operation) {
            case "MEAL_REPLACED" -> MealSelectionSource.MANUALLY_REPLACED;
            case "MEAL_REGENERATED" -> MealSelectionSource.PARTIALLY_REGENERATED;
            default -> MealSelectionSource.DAY_REGENERATED;
        };
        var updatedMeals = preview.afterMeals().stream().map(meal ->
                withEditMetadata(meal, selectionSource, plan.getEditVersion(), preview.seed(), now)
        ).toList();
        var days = replaceMeals(source.days(), updatedMeals);
        var recalculated = recalculationService.recalculate(
                source, days, plan.getEditVersion(), plan.getContentVersion(), now
        );
        applyMeals(plan, updatedMeals, plan.getEditVersion(), preview.seed(), now, false);
        updateDayTotals(plan, recalculated);
        var finalResult = snapshotService.decorate(plan, recalculated);
        finalResult = copyVersions(finalResult, plan, now);
        plan.updateEditedSnapshot(finalResult, json(finalResult), now);
        var type = MealPlanChangeType.valueOf(operation);
        var dayId = updatedMeals.size() == 1
                ? findPersistedMeal(plan, updatedMeals.getFirst().plannedMealId()).getMealPlanDay().getId()
                : preview.targetId();
        var event = change(
                plan, type, beforeVersion, dayId,
                updatedMeals.size() == 1 ? updatedMeals.getFirst().plannedMealId() : null,
                preview.beforeMeals(), updatedMeals, source, finalResult,
                preview.seed(), operationReason(type), now
        );
        changeRepository.save(event);
        planRepository.saveAndFlush(plan);
        return snapshotService.decorate(plan, finalResult);
    }

    private EditPreviewResponse preview(
            EditContext context,
            List<GeneratedMealPlanResult.PlannedMealResult> replacements,
            String operation,
            long seed
    ) {
        var started = System.nanoTime();
        var source = context.source();
        var days = replaceMeals(source.days(), replacements);
        var after = recalculationService.recalculate(
                source, days, source.editVersion(), source.contentVersion(), source.updatedAt()
        );
        var beforeMeals = affectedCurrent(source, replacements);
        var beforeMetrics = metrics(source);
        var afterMetrics = metrics(after);
        var delta = delta(beforeMetrics, afterMetrics);
        var targetId = "DAY_REGENERATED".equals(operation)
                ? context.day().getId() : context.meal().getId();
        var signed = tokenService.issue(new EditPreviewTokenService.TokenPayload(
                operation, context.plan().getId(), currentUser.userId(), targetId,
                context.plan().getEditVersion(),
                replacements.stream().map(GeneratedMealPlanResult.PlannedMealResult::templateId).toList(),
                seed, hash(replacements), source.strategy().name(),
                source.generationMetadata().optimizationPreset() == null
                        ? null : source.generationMetadata().optimizationPreset().name(),
                hash(source.days()), afterMetrics,
                source.generationMetadata().algorithmVersion(), 0
        ));
        return new EditPreviewResponse(
                operation, context.plan().getId(), targetId, context.plan().getEditVersion(),
                seed, beforeMeals, replacements, beforeMetrics, afterMetrics, delta,
                reasons(delta), after.purchaseMetrics() == null
                        ? List.of() : after.purchaseMetrics().warnings(),
                signed.value(), OffsetDateTime.ofInstant(signed.expiresAt(), ZoneOffset.UTC),
                (System.nanoTime() - started) / 1_000_000
        );
    }

    private List<GeneratedMealPlanResult.PlannedMealResult> candidateMeals(
            EditContext context,
            long seed
    ) {
        var criteria = context.source().criteria();
        var templates = templateService.findAll(new MealTemplateSearchCriteria(
                SupermarketCode.valueOf(context.source().supermarketCode()),
                MealType.valueOf(context.current().mealType()), true, null, null, null,
                criteria.maximumPreparationMinutes(), criteria.excludedAllergens(),
                criteria.requiredDietaryTags(), 0, 100, "name", false
        )).content();
        var currentCounts = context.source().days().stream().flatMap(day -> day.meals().stream())
                .filter(meal -> !meal.plannedMealId().equals(context.meal().getId()))
                .collect(Collectors.groupingBy(
                        GeneratedMealPlanResult.PlannedMealResult::templateId,
                        Collectors.counting()
                ));
        return templates.stream()
                .filter(template -> !template.id().equals(context.current().templateId()))
                .filter(template -> !criteria.excludedTemplateIds().contains(template.id()))
                .filter(template -> currentCounts.getOrDefault(template.id(), 0L)
                        < criteria.maximumTemplateRepetitions())
                .map(template -> toPlannedMeal(context.current(), template))
                .filter(meal -> criteria.allowIncompleteCalculations() || meal.calculationComplete())
                .filter(meal -> meal.ingredients().stream().noneMatch(ingredient ->
                        criteria.excludedProductIds().contains(ingredient.productId())
                                || Boolean.FALSE.equals(ingredient.available())))
                .sorted(Comparator.comparing(
                        (GeneratedMealPlanResult.PlannedMealResult meal) ->
                                tieKey(seed, meal.templateId()), Long::compareUnsigned
                ).thenComparing(GeneratedMealPlanResult.PlannedMealResult::templateId))
                .toList();
    }

    private GeneratedMealPlanResult.PlannedMealResult toPlannedMeal(
            GeneratedMealPlanResult.PlannedMealResult current,
            MealTemplateResponse original
    ) {
        var mandatoryIngredients = original.ingredients().stream()
                .filter(value -> !value.optional())
                .map(value -> new MealTemplateIngredientRequest(
                        value.productId(), value.quantity(),
                        QuantityUnit.valueOf(value.quantityUnit()), false,
                        value.sortOrder(), value.notes()
                )).toList();
        var mandatory = templateService.preview(new MealTemplateRequest(
                original.supermarketCode(), original.name(), original.description(),
                MealType.valueOf(original.mealType()), original.instructions(),
                original.preparationMinutes(), original.servings(), true,
                original.imageUrl(), mandatoryIngredients
        ));
        var productIds = mandatoryIngredients.stream()
                .map(MealTemplateIngredientRequest::productId).collect(Collectors.toSet());
        var products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        var servings = current.servings();
        var ingredientMultiplier = BigDecimal.valueOf(servings)
                .divide(BigDecimal.valueOf(original.servings()), 12, RoundingMode.HALF_UP);
        var ingredients = original.ingredients().stream().filter(value -> !value.optional())
                .map(value -> {
                    var product = products.get(value.productId());
                    return new GeneratedMealPlanResult.IngredientSummary(
                            value.productId(), value.productName(), value.brand(),
                            product == null ? null : product.getCategory().getId(),
                            product == null ? value.category() : product.getCategory().getName(),
                            value.quantity().multiply(ingredientMultiplier).setScale(3, RoundingMode.HALF_UP)
                                    .stripTrailingZeros(),
                            value.quantityUnit(),
                            product == null ? null : product.getMeasurementType().name(),
                            product == null ? null : product.getPackageQuantity(),
                            product == null || product.getPackageUnit() == null
                                    ? null : product.getPackageUnit().name(),
                            product == null ? null : product.getCurrentPrice(),
                            product == null ? null : product.getUnitPrice(),
                            product == null ? null : product.isAvailable(),
                            value.calculatedConsumedCost() == null ? null
                                    : value.calculatedConsumedCost().multiply(ingredientMultiplier)
                                            .setScale(2, RoundingMode.HALF_UP),
                            product != null && value.costCalculationComplete(),
                            value.warnings(), "MEAL_TOTAL"
                    );
                }).toList();
        var nutrition = multiply(mandatory.nutritionPerServing(), BigDecimal.valueOf(servings));
        var cost = mandatory.consumedCostPerServing().multiply(BigDecimal.valueOf(servings))
                .setScale(2, RoundingMode.HALF_UP);
        return new GeneratedMealPlanResult.PlannedMealResult(
                current.position(), current.mealType(), original.id(), original.name(),
                servings, original.preparationMinutes(), ingredients, nutrition, cost,
                BigDecimal.ZERO, mandatory.calculationComplete(), mandatory.warnings(),
                current.plannedMealId(), current.locked(), current.selectionSource(),
                current.editVersion(), current.modifiedAt(), current.originalMealTemplateId(),
                current.partialGenerationSeed()
        );
    }

    private AlternativeResponse evaluatedAlternative(
            EditContext context,
            GeneratedMealPlanResult.PlannedMealResult candidate,
            long seed
    ) {
        var after = recalculationService.recalculate(
                context.source(), replaceMeals(context.source().days(), List.of(candidate)),
                context.source().editVersion(), context.source().contentVersion(),
                context.source().updatedAt()
        );
        var before = metrics(context.source());
        var afterMetrics = metrics(after);
        var delta = delta(before, afterMetrics);
        return new AlternativeResponse(
                0, candidate.templateId(), candidate.templateName(),
                candidate.ingredients().stream().limit(4)
                        .map(GeneratedMealPlanResult.IngredientSummary::productName).toList(),
                candidate.nutrition().calories(), candidate.nutrition().protein(),
                candidate.consumedCost(),
                positive(delta.purchaseCost()), delta.purchaseCost(), delta.wasteCost(),
                integer(delta.packages()), integer(delta.uniqueProducts()),
                delta.varietyScore(), delta.repetitionScore(), after.overallScore(),
                reasons(delta), candidate.warnings(), seed
        );
    }

    private Comparator<AlternativeResponse> alternativeComparator(AlternativePriority priority) {
        return switch (priority == null ? AlternativePriority.BEST_BALANCE : priority) {
            case LOWER_PURCHASE_COST -> Comparator.comparing(
                    AlternativeResponse::purchaseCostDelta,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case LOWER_WASTE -> Comparator.comparing(
                    AlternativeResponse::wasteCostDelta,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case MORE_VARIETY -> Comparator.comparing(
                    AlternativeResponse::varietyDelta,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
            case BEST_BALANCE -> Comparator.comparing(
                    AlternativeResponse::estimatedScore,
                    Comparator.reverseOrder()
            );
        };
    }

    private void applyMeals(
            MealPlanEntity plan,
            List<GeneratedMealPlanResult.PlannedMealResult> meals,
            long version,
            Long seed,
            OffsetDateTime now,
            boolean restoring
    ) {
        var templates = templateRepository.findAllById(
                meals.stream().map(GeneratedMealPlanResult.PlannedMealResult::templateId).toList()
        ).stream().collect(Collectors.toMap(MealTemplateEntity::getId, Function.identity()));
        for (var meal : meals) {
            var entity = findPersistedMeal(plan, meal.plannedMealId());
            if (!restoring) ensureUnlocked(entity);
            var source = restoring
                    ? meal.selectionSource()
                    : meal.selectionSource();
            entity.replace(
                    templates.get(meal.templateId()), meal,
                    source == null ? MealSelectionSource.GENERATED : source,
                    version, seed, now
            );
        }
    }

    private void updateDayTotals(MealPlanEntity plan, GeneratedMealPlanResult result) {
        var byIndex = result.days().stream().collect(Collectors.toMap(
                GeneratedMealPlanResult.DayResult::dayIndex, Function.identity()
        ));
        plan.getDays().forEach(day -> day.updateTotals(byIndex.get(day.getDayIndex())));
    }

    private EditContext context(UUID planId, UUID mealId) {
        var plan = findPlan(planId);
        var source = snapshotService.read(plan);
        var meal = findPersistedMeal(plan, mealId);
        return context(plan, meal, source);
    }

    private EditContext context(
            MealPlanEntity plan,
            PlannedMealEntity meal,
            GeneratedMealPlanResult source
    ) {
        return new EditContext(
                plan, meal.getMealPlanDay(), meal, source, findMeal(source, meal.getId())
        );
    }

    private MealPlanEntity findPlan(UUID id) {
        return planRepository.findByIdAndOwnerId(id, currentUser.userId())
                .orElseThrow(() -> new MealPlanEditingException(
                "Meal plan not found: " + id, "MEAL_PLAN_NOT_FOUND", 404
        ));
    }

    private MealPlanDayEntity findDay(MealPlanEntity plan, UUID id) {
        return plan.getDays().stream().filter(day -> day.getId().equals(id)).findFirst()
                .orElseThrow(() -> new MealPlanEditingException(
                        "Meal-plan day not found: " + id, "MEAL_PLAN_DAY_NOT_FOUND", 404
                ));
    }

    private PlannedMealEntity findPersistedMeal(MealPlanEntity plan, UUID id) {
        return plan.getDays().stream().flatMap(day -> day.getMeals().stream())
                .filter(meal -> meal.getId().equals(id)).findFirst()
                .orElseThrow(() -> new MealPlanEditingException(
                        "Planned meal not found: " + id, "PLANNED_MEAL_NOT_FOUND", 404
                ));
    }

    private GeneratedMealPlanResult.PlannedMealResult findMeal(
            GeneratedMealPlanResult plan,
            UUID id
    ) {
        return plan.days().stream().flatMap(day -> day.meals().stream())
                .filter(meal -> id.equals(meal.plannedMealId())).findFirst()
                .orElseThrow(() -> new MealPlanEditingException(
                        "Planned meal snapshot not found: " + id,
                        "PLANNED_MEAL_NOT_FOUND",
                        404
                ));
    }

    private void ensureEditable(MealPlanEntity plan) {
        if (plan.getStatus() != MealPlanStatus.GENERATED) {
            throw new MealPlanEditingException(
                    "Only generated plans can be edited",
                    "MEAL_PLAN_NOT_EDITABLE",
                    422
            );
        }
    }

    private void ensureUnlocked(PlannedMealEntity meal) {
        if (meal.isLocked()) {
            throw new MealPlanEditingException(
                    "The planned meal is locked",
                    "PLANNED_MEAL_LOCKED",
                    422
            );
        }
    }

    private void validateVersion(MealPlanEntity plan, long expected) {
        if (plan.getEditVersion() != expected) {
            throw new MealPlanEditingException(
                    "Expected editVersion " + expected + " but current version is "
                            + plan.getEditVersion(),
                    "MEAL_PLAN_VERSION_CONFLICT",
                    409
            );
        }
    }

    private MealPlanEditingException noAlternative() {
        return new MealPlanEditingException(
                "No valid alternative exists for the requested target",
                "NO_VALID_ALTERNATIVE",
                422
        );
    }

    private MealPlanEditingException stale(String message) {
        return new MealPlanEditingException(message, "EDIT_PREVIEW_STALE", 409);
    }

    private List<GeneratedMealPlanResult.DayResult> replaceMeals(
            List<GeneratedMealPlanResult.DayResult> days,
            List<GeneratedMealPlanResult.PlannedMealResult> replacements
    ) {
        var byId = replacements.stream().collect(Collectors.toMap(
                GeneratedMealPlanResult.PlannedMealResult::plannedMealId,
                Function.identity()
        ));
        return days.stream().map(day -> new GeneratedMealPlanResult.DayResult(
                day.dayIndex(), day.date(),
                day.meals().stream().map(meal ->
                        byId.getOrDefault(meal.plannedMealId(), meal)
                ).toList(),
                day.totalNutrition(), day.totalConsumedCost(), day.calorieTarget(),
                day.proteinTarget(), day.calorieDeviation(), day.calorieDeviationPercentage(),
                day.proteinDeviation(), day.dailyScore(), day.warnings(), day.dayId()
        )).toList();
    }

    private List<GeneratedMealPlanResult.PlannedMealResult> affectedCurrent(
            GeneratedMealPlanResult source,
            List<GeneratedMealPlanResult.PlannedMealResult> replacements
    ) {
        return replacements.stream()
                .map(value -> findMeal(source, value.plannedMealId()))
                .toList();
    }

    private GeneratedMealPlanResult.PlannedMealResult withEditMetadata(
            GeneratedMealPlanResult.PlannedMealResult meal,
            MealSelectionSource source,
            long version,
            long seed,
            OffsetDateTime now
    ) {
        return new GeneratedMealPlanResult.PlannedMealResult(
                meal.position(), meal.mealType(), meal.templateId(), meal.templateName(),
                meal.servings(), meal.preparationMinutes(), meal.ingredients(), meal.nutrition(),
                meal.consumedCost(), meal.score(), meal.calculationComplete(), meal.warnings(),
                meal.plannedMealId(), meal.locked(), source, version, now,
                meal.originalMealTemplateId() == null ? null : meal.originalMealTemplateId(), seed
        );
    }

    private GeneratedMealPlanResult copyVersions(
            GeneratedMealPlanResult source,
            MealPlanEntity plan,
            OffsetDateTime now
    ) {
        return new GeneratedMealPlanResult(
                source.persisted(), source.mealPlanId(), source.generationToken(), source.name(),
                source.supermarketCode(), source.supermarketName(), source.startDate(),
                source.numberOfDays(), source.mealsPerDay(), source.servings(), source.seed(),
                source.strategy(), source.status(), source.criteria(), source.days(),
                source.weeklyNutrition(), source.totalConsumedCost(), source.purchaseMetrics(),
                source.weeklyBudget(), source.budgetDifference(), source.budgetExceeded(),
                source.budgetDeviationPercentage(), source.overallScore(), source.scoreBreakdown(),
                source.varietyMetrics(), source.calculationComplete(), source.warnings(),
                source.constraintsApplied(), source.constraintsNotMet(),
                source.rejectedCandidateStatistics(), source.generationMetadata(),
                source.createdAt(), now, plan.getEditVersion(), plan.getContentVersion(),
                source.shoppingListStatus(), source.activeShoppingListId(),
                source.canUndo(), source.lastChangeSummary()
        );
    }

    private MealPlanChangeEntity change(
            MealPlanEntity plan,
            MealPlanChangeType type,
            MealPlanEntity.VersionChange beforeVersion,
            UUID dayId,
            UUID mealId,
            List<GeneratedMealPlanResult.PlannedMealResult> beforeMeals,
            List<GeneratedMealPlanResult.PlannedMealResult> afterMeals,
            GeneratedMealPlanResult before,
            GeneratedMealPlanResult after,
            Long seed,
            String reason,
            OffsetDateTime now
    ) {
        var beforeMetrics = metrics(before);
        var afterMetrics = metrics(after);
        return new MealPlanChangeEntity(
                plan, changeRepository.countByMealPlanId(plan.getId()) + 1, type,
                beforeVersion, dayId, mealId, objectMapper.valueToTree(beforeMeals),
                objectMapper.valueToTree(afterMeals), objectMapper.valueToTree(beforeMetrics),
                objectMapper.valueToTree(afterMetrics),
                objectMapper.valueToTree(delta(beforeMetrics, afterMetrics)), seed,
                before.strategy(), before.generationMetadata().optimizationPreset(),
                reason, now
        );
    }

    private ChangeResponse changeResponse(MealPlanChangeEntity value) {
        return new ChangeResponse(
                value.getId(), value.getSequenceNumber(), value.getChangeType(),
                value.getEditVersionAfter(), value.getContentVersionAfter(),
                value.getMealPlanDayId(), value.getPlannedMealId(),
                objectMapper.convertValue(value.getMetricsBefore(), MetricsSnapshot.class),
                objectMapper.convertValue(value.getMetricsAfter(), MetricsSnapshot.class),
                objectMapper.convertValue(value.getMetricsDelta(), MetricsSnapshot.class),
                value.getDeterministicSeed(), value.getGenerationStrategy().name(),
                value.getOptimizationPreset() == null ? null : value.getOptimizationPreset().name(),
                value.getReason(), value.getUndoneByChangeId() != null, value.getCreatedAt()
        );
    }

    private List<GeneratedMealPlanResult.PlannedMealResult> meals(JsonNode node) {
        return objectMapper.convertValue(
                node,
                new TypeReference<List<GeneratedMealPlanResult.PlannedMealResult>>() { }
        );
    }

    private MetricsSnapshot metrics(GeneratedMealPlanResult plan) {
        var purchase = plan.purchaseMetrics();
        return new MetricsSnapshot(
                plan.weeklyNutrition().calories(), plan.weeklyNutrition().protein(),
                plan.totalConsumedCost(),
                purchase == null ? null : purchase.estimatedPurchaseCost(),
                purchase == null ? null : purchase.estimatedWasteCost(),
                purchase == null ? null : purchase.estimatedWastePercentage(),
                purchase == null ? null : purchase.estimatedPackageCount(),
                purchase == null ? null : purchase.estimatedUniqueProductCount(),
                plan.scoreBreakdown().varietyScore(), plan.scoreBreakdown().repetitionScore(),
                plan.overallScore(),
                purchase == null ? plan.budgetDifference() : purchase.purchaseBudgetDifference(),
                purchase == null ? plan.budgetExceeded() : purchase.purchaseBudgetExceeded()
        );
    }

    private MetricsSnapshot delta(MetricsSnapshot before, MetricsSnapshot after) {
        return new MetricsSnapshot(
                subtract(after.calories(), before.calories()),
                subtract(after.protein(), before.protein()),
                subtract(after.consumedCost(), before.consumedCost()),
                subtract(after.purchaseCost(), before.purchaseCost()),
                subtract(after.wasteCost(), before.wasteCost()),
                subtract(after.wastePercentage(), before.wastePercentage()),
                subtract(after.packages(), before.packages()),
                subtract(after.uniqueProducts(), before.uniqueProducts()),
                subtract(after.varietyScore(), before.varietyScore()),
                subtract(after.repetitionScore(), before.repetitionScore()),
                subtract(after.overallScore(), before.overallScore()),
                subtract(after.budgetDifference(), before.budgetDifference()),
                after.budgetExceeded()
        );
    }

    private List<String> reasons(MetricsSnapshot delta) {
        var values = new ArrayList<String>();
        if (negative(delta.purchaseCost())) values.add("Reduce el coste real de compra");
        if (negative(delta.wasteCost())) values.add("Aprovecha sobrantes y reduce desperdicio");
        if (positiveSign(delta.varietyScore())) values.add("Mejora la variedad semanal");
        if (positiveSign(delta.overallScore())) values.add("Mejora la puntuación global");
        if (values.isEmpty()) values.add("Alternativa válida para las restricciones del plan");
        return List.copyOf(values);
    }

    private String hash(Object value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(value));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash edit preview", exception);
        }
    }

    private long derivedSeed(UUID planId, long editVersion, UUID targetId) {
        try {
            var bytes = MessageDigest.getInstance("SHA-256").digest(
                    (planId + "|" + editVersion + "|" + targetId)
                            .getBytes(StandardCharsets.UTF_8)
            );
            return java.nio.ByteBuffer.wrap(bytes).getLong();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not derive partial generation seed", exception);
        }
    }

    private long tieKey(long seed, UUID templateId) {
        var value = seed ^ templateId.getMostSignificantBits() ^ templateId.getLeastSignificantBits();
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        return value;
    }

    private String replacementKey(List<GeneratedMealPlanResult.PlannedMealResult> values) {
        return values.stream().map(value -> value.templateId().toString())
                .collect(Collectors.joining("|"));
    }

    private NutritionBreakdown multiply(NutritionBreakdown value, BigDecimal factor) {
        return new NutritionBreakdown(
                scaled(value.calories(), factor), scaled(value.protein(), factor),
                scaled(value.carbohydrates(), factor), scaled(value.fat(), factor),
                scaled(value.fiber(), factor), scaled(value.sugar(), factor),
                scaled(value.salt(), factor)
        );
    }

    private BigDecimal scaled(BigDecimal value, BigDecimal factor) {
        return value.multiply(factor).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
    }
    private BigDecimal subtract(BigDecimal after, BigDecimal before) {
        return after == null || before == null ? null : after.subtract(before);
    }
    private Integer subtract(Integer after, Integer before) {
        return after == null || before == null ? null : after - before;
    }
    private BigDecimal positive(BigDecimal value) {
        return value == null ? null : value.max(BigDecimal.ZERO);
    }
    private int integer(Integer value) { return value == null ? 0 : value; }
    private boolean negative(BigDecimal value) { return value != null && value.signum() < 0; }
    private boolean positiveSign(BigDecimal value) { return value != null && value.signum() > 0; }
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize edited meal plan", exception);
        }
    }
    private String operationReason(MealPlanChangeType type) {
        return switch (type) {
            case MEAL_REPLACED -> "Comida sustituida manualmente";
            case MEAL_REGENERATED -> "Comida regenerada";
            case DAY_REGENERATED -> "Día regenerado";
            default -> type.name();
        };
    }

    private record EditContext(
            MealPlanEntity plan,
            MealPlanDayEntity day,
            PlannedMealEntity meal,
            GeneratedMealPlanResult source,
            GeneratedMealPlanResult.PlannedMealResult current
    ) {
    }

    private record DayState(
            List<GeneratedMealPlanResult.PlannedMealResult> replacements,
            GeneratedMealPlanResult result
    ) {
    }
}
