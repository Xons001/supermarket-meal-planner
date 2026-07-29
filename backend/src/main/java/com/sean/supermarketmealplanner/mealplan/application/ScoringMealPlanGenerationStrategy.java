package com.sean.supermarketmealplanner.mealplan.application;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.AllergenRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.DietaryTagRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus;
import com.sean.supermarketmealplanner.mealplan.domain.WarningSeverity;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateIngredientRequest;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateRequest;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateResponse;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateSearchCriteria;
import com.sean.supermarketmealplanner.mealtemplate.application.MealTemplateService;
import com.sean.supermarketmealplanner.mealtemplate.application.NutritionBreakdown;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateRepository;
import com.sean.supermarketmealplanner.shared.application.purchase.PurchaseMetricsCalculator;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoringMealPlanGenerationStrategy implements MealPlanGenerationStrategy {

    static final String ALGORITHM_VERSION = "scoring-beam-v1";
    static final String PURCHASE_AWARE_ALGORITHM_VERSION = "purchase-aware-beam-v1";
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final SecureRandom SEED_SOURCE = new SecureRandom();
    private static final long MAX_JAVASCRIPT_SAFE_INTEGER = 9_007_199_254_740_991L;
    private static final Map<Integer, List<MealType>> DEFAULT_DISTRIBUTIONS = Map.of(
            1, List.of(MealType.LUNCH),
            2, List.of(MealType.LUNCH, MealType.DINNER),
            3, List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER),
            4, List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACK, MealType.DINNER),
            5, List.of(
                    MealType.BREAKFAST,
                    MealType.SNACK,
                    MealType.LUNCH,
                    MealType.SNACK,
                    MealType.DINNER
            ),
            6, List.of(
                    MealType.BREAKFAST,
                    MealType.SNACK,
                    MealType.LUNCH,
                    MealType.SNACK,
                    MealType.DINNER,
                    MealType.SNACK
            )
    );

    private final MealTemplateService mealTemplateService;
    private final MealTemplateRepository mealTemplateRepository;
    private final ProductRepository productRepository;
    private final SupermarketRepository supermarketRepository;
    private final DietaryTagRepository dietaryTagRepository;
    private final AllergenRepository allergenRepository;
    private final MealPlanScoringService scoringService;
    private final MealPlanScoringProperties properties;
    private final PurchaseMetricsCalculator purchaseCalculator;
    private final PurchaseAwareMealPlanScoringService purchaseScoringService;

    public ScoringMealPlanGenerationStrategy(
            MealTemplateService mealTemplateService,
            MealTemplateRepository mealTemplateRepository,
            ProductRepository productRepository,
            SupermarketRepository supermarketRepository,
            DietaryTagRepository dietaryTagRepository,
            AllergenRepository allergenRepository,
            MealPlanScoringService scoringService,
            MealPlanScoringProperties properties,
            PurchaseMetricsCalculator purchaseCalculator,
            PurchaseAwareMealPlanScoringService purchaseScoringService
    ) {
        this.mealTemplateService = mealTemplateService;
        this.mealTemplateRepository = mealTemplateRepository;
        this.productRepository = productRepository;
        this.supermarketRepository = supermarketRepository;
        this.dietaryTagRepository = dietaryTagRepository;
        this.allergenRepository = allergenRepository;
        this.scoringService = scoringService;
        this.properties = properties;
        this.purchaseCalculator = purchaseCalculator;
        this.purchaseScoringService = purchaseScoringService;
    }

    @Override
    public GenerationStrategy supportedStrategy() {
        return GenerationStrategy.SCORING;
    }

    @Override
    @Transactional(readOnly = true)
    public GeneratedMealPlanResult generate(GenerateMealPlanCommand originalCommand) {
        return generateInternal(originalCommand, false);
    }

    @Transactional(readOnly = true)
    public GeneratedMealPlanResult generatePurchaseAware(
            GenerateMealPlanCommand originalCommand
    ) {
        return generateInternal(originalCommand, true);
    }

    private GeneratedMealPlanResult generateInternal(
            GenerateMealPlanCommand originalCommand,
            boolean purchaseAware
    ) {
        var started = System.nanoTime();
        validateCommand(originalCommand);
        var seed = originalCommand.deterministicSeed() == null
                ? generatedSafeSeed()
                : originalCommand.deterministicSeed();
        var command = originalCommand.withSeedAndPersistence(
                seed,
                originalCommand.generationToken(),
                originalCommand.persist()
        );
        var supermarketCode = parseSupermarket(command.supermarketCode());
        var supermarket = supermarketRepository.findByCode(supermarketCode)
                .filter(value -> value.isEnabled())
                .orElseThrow(() -> new MealPlanValidationException(
                        "Invalid or disabled supermarketCode: " + command.supermarketCode()
                ));
        validateReferences(command);

        var rejected = new LinkedHashMap<String, Integer>();
        var rawTemplates = findTemplates(command, supermarketCode, Set.of(), Set.of());
        var allowedByDietAndAllergens = findTemplates(
                command,
                supermarketCode,
                command.excludedAllergens(),
                command.requiredDietaryTags()
        ).stream().map(MealTemplateResponse::id).collect(Collectors.toSet());
        var products = productRepository.findAllById(rawTemplates.stream()
                        .flatMap(template -> template.ingredients().stream())
                        .map(ingredient -> ingredient.productId())
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        var candidates = new ArrayList<Candidate>();
        for (var template : rawTemplates) {
            if (!command.allowedMealTypes().contains(MealType.valueOf(template.mealType()))) {
                increment(rejected, "mealType");
                continue;
            }
            if (!allowedByDietAndAllergens.contains(template.id())) {
                increment(rejected, "dietaryTagsOrAllergens");
                continue;
            }
            if (command.excludedTemplateIds().contains(template.id())) {
                increment(rejected, "excludedTemplate");
                continue;
            }
            var requiredIngredients = template.ingredients().stream()
                    .filter(ingredient -> !ingredient.optional())
                    .toList();
            if (requiredIngredients.stream()
                    .anyMatch(ingredient -> command.excludedProductIds()
                            .contains(ingredient.productId()))) {
                increment(rejected, "excludedProduct");
                continue;
            }
            if (requiredIngredients.stream()
                    .map(ingredient -> products.get(ingredient.productId()))
                    .anyMatch(product -> product == null || !product.isAvailable())) {
                increment(rejected, "unavailableProduct");
                continue;
            }
            var mandatoryOnly = calculateMandatoryOnly(template);
            if (!mandatoryOnly.calculationComplete() && !command.allowIncompleteCalculations()) {
                increment(rejected, "incompleteCalculation");
                continue;
            }
            candidates.add(toCandidate(
                    template,
                    mandatoryOnly,
                    command.servings(),
                    products
            ));
        }
        candidates.sort(Comparator.comparing(Candidate::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Candidate::templateId));

        var byType = new EnumMap<MealType, List<Candidate>>(MealType.class);
        for (var type : MealType.values()) {
            byType.put(type, candidates.stream().filter(value -> value.mealType() == type).toList());
        }
        var adaptedTypes = new ArrayList<String>();
        var slots = buildSlots(command, byType, adaptedTypes);
        if (candidates.isEmpty()) {
            throw impossible(
                    "No meal templates remain after applying the requested restrictions",
                    byType,
                    rejected,
                    command
            );
        }

        var evaluated = new AtomicInteger();
        var initialPurchaseState = purchaseAware
                ? purchaseCalculator.empty(command.weeklyBudget())
                : null;
        var beam = List.of(new BeamState(
                List.of(),
                BigDecimal.ZERO,
                seed,
                initialPurchaseState,
                0
        ));
        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            var slot = slots.get(slotIndex);
            var slotCandidates = byType.getOrDefault(slot.mealType(), List.of());
            if (slotCandidates.isEmpty()) {
                throw impossible(
                        "No candidates exist for required meal type " + slot.mealType(),
                        byType,
                        rejected,
                        command
                );
            }
            var rankedForSlot = slotCandidates.stream()
                    .sorted(candidateComparator(command, slot, slotIndex, seed))
                    .limit(properties.getCandidatesPerPosition())
                    .toList();
            var expanded = new ArrayList<BeamState>();
            for (var state : beam) {
                for (var candidate : rankedForSlot) {
                    evaluated.incrementAndGet();
                    var selections = new ArrayList<>(state.selections());
                    selections.add(new Selection(slot, candidate));
                    PurchaseMetricsCalculator.PurchaseDelta purchaseDelta = null;
                    if (purchaseAware) {
                        purchaseDelta = purchaseCalculator.add(
                                state.purchaseState(),
                                candidate.ingredients().stream().map(this::purchaseInput).toList(),
                                PurchaseMetricsCalculator.ConflictMode.LENIENT
                        );
                    }
                    var usefulReuse = state.economicallyUsefulReuseCount()
                            + (purchaseDelta == null
                                    ? 0
                                    : purchaseDelta.economicallyUsefulReuse());
                    var penalty = partialPenalty(command, selections);
                    if (purchaseDelta != null) {
                        penalty = penalty.add(purchaseScoringService.partialPenalty(
                                        command,
                                        purchaseDelta.state().calculation(),
                                        usefulReuse,
                                        selections.size()
                                ))
                                .subtract(purchaseScoringService.marginalBonus(
                                        command,
                                        purchaseDelta
                                ));
                    }
                    expanded.add(new BeamState(
                            List.copyOf(selections),
                            penalty,
                            sequenceKey(seed, selections),
                            purchaseDelta == null ? null : purchaseDelta.state(),
                            usefulReuse
                    ));
                }
            }
            beam = expanded.stream()
                    .sorted(Comparator.comparing(BeamState::penalty)
                            .thenComparing(BeamState::tieKey, Long::compareUnsigned))
                    .limit(properties.getBeamWidth())
                    .toList();
        }

        var finalStates = beam.stream().map(state -> {
            var legacyScore = scoringService.score(
                    command,
                    toScoredMeals(state.selections())
            );
            var purchaseScore = purchaseAware
                    ? purchaseScoringService.score(
                            command,
                            legacyScore,
                            state.purchaseState().calculation(),
                            state.economicallyUsefulReuseCount()
                    )
                    : null;
            return new FinalState(state, legacyScore, purchaseScore);
        }).sorted(Comparator.comparing(
                        FinalState::totalScore
                ).reversed().thenComparing(value -> value.state().tieKey(), Long::compareUnsigned))
                .toList();
        if (finalStates.isEmpty()) {
            throw impossible("The generator could not complete a plan", byType, rejected, command);
        }
        var best = finalStates.getFirst();
        var generatedAt = OffsetDateTime.now();
        var token = generationToken(command, candidates);
        if (command.generationToken() != null
                && !command.generationToken().isBlank()
                && !command.generationToken().equals(token)) {
            throw new MealPlanValidationException(
                    "The generationToken no longer matches the available template data; "
                            + "generate a new preview"
            );
        }
        var duration = (System.nanoTime() - started) / 1_000_000;
        return buildResult(
                command,
                supermarket.getName(),
                best,
                token,
                rejected,
                adaptedTypes,
                evaluated.get(),
                finalStates.size(),
                duration,
                generatedAt,
                purchaseAware
        );
    }

    private void validateCommand(GenerateMealPlanCommand command) {
        if (command.numberOfDays() < 1 || command.numberOfDays() > 14) {
            throw new MealPlanValidationException("numberOfDays must be between 1 and 14");
        }
        if (command.mealsPerDay() < 1 || command.mealsPerDay() > 6) {
            throw new MealPlanValidationException("mealsPerDay must be between 1 and 6");
        }
        if (command.servings() <= 0) {
            throw new MealPlanValidationException("servings must be greater than zero");
        }
        if (command.dailyCaloriesTarget() == null
                || command.dailyCaloriesTarget().signum() <= 0) {
            throw new MealPlanValidationException("dailyCaloriesTarget must be positive");
        }
        if (command.dailyProteinTarget() == null
                || command.dailyProteinTarget().signum() < 0) {
            throw new MealPlanValidationException("dailyProteinTarget must be non-negative");
        }
        if (command.weeklyBudget() != null && command.weeklyBudget().signum() <= 0) {
            throw new MealPlanValidationException("weeklyBudget must be positive");
        }
        if (command.allowedMealTypes().isEmpty()) {
            throw new MealPlanValidationException("allowedMealTypes must not be empty");
        }
    }

    private long generatedSafeSeed() {
        var magnitude = SEED_SOURCE.nextLong(MAX_JAVASCRIPT_SAFE_INTEGER);
        return SEED_SOURCE.nextBoolean() ? magnitude : -magnitude;
    }

    private void validateReferences(GenerateMealPlanCommand command) {
        var tags = dietaryTagRepository.findAllByCodeIn(command.requiredDietaryTags()).stream()
                .map(value -> value.getCode()).collect(Collectors.toSet());
        var invalidTags = new LinkedHashSet<>(command.requiredDietaryTags());
        invalidTags.removeAll(tags);
        if (!invalidTags.isEmpty()) {
            throw new MealPlanValidationException("Invalid dietary tags: " + invalidTags);
        }
        var allergens = allergenRepository.findAllByCodeIn(command.excludedAllergens()).stream()
                .map(value -> value.getCode()).collect(Collectors.toSet());
        var invalidAllergens = new LinkedHashSet<>(command.excludedAllergens());
        invalidAllergens.removeAll(allergens);
        if (!invalidAllergens.isEmpty()) {
            throw new MealPlanValidationException("Invalid allergens: " + invalidAllergens);
        }
        var existingTemplates = mealTemplateRepository.findAllById(command.excludedTemplateIds())
                .stream().map(value -> value.getId()).collect(Collectors.toSet());
        var missingTemplates = new LinkedHashSet<>(command.excludedTemplateIds());
        missingTemplates.removeAll(existingTemplates);
        if (!missingTemplates.isEmpty()) {
            throw new MealPlanValidationException("Excluded meal template not found: "
                    + missingTemplates.iterator().next());
        }
        var existingProducts = productRepository.findAllById(command.excludedProductIds())
                .stream().map(ProductEntity::getId).collect(Collectors.toSet());
        var missingProducts = new LinkedHashSet<>(command.excludedProductIds());
        missingProducts.removeAll(existingProducts);
        if (!missingProducts.isEmpty()) {
            throw new MealPlanValidationException("Excluded product not found: "
                    + missingProducts.iterator().next());
        }
    }

    private List<MealTemplateResponse> findTemplates(
            GenerateMealPlanCommand command,
            SupermarketCode supermarketCode,
            Set<String> allergens,
            Set<String> tags
    ) {
        return mealTemplateService.findAll(new MealTemplateSearchCriteria(
                supermarketCode,
                null,
                true,
                null,
                null,
                null,
                command.maximumPreparationMinutes(),
                allergens,
                tags,
                0,
                48,
                "name",
                false
        )).content();
    }

    private MealTemplateResponse calculateMandatoryOnly(MealTemplateResponse template) {
        var ingredients = template.ingredients().stream()
                .filter(ingredient -> !ingredient.optional())
                .map(ingredient -> new MealTemplateIngredientRequest(
                        ingredient.productId(),
                        ingredient.quantity(),
                        com.sean.supermarketmealplanner.mealtemplate.domain.QuantityUnit.valueOf(
                                ingredient.quantityUnit()
                        ),
                        false,
                        ingredient.sortOrder(),
                        ingredient.notes()
                ))
                .toList();
        return mealTemplateService.preview(new MealTemplateRequest(
                template.supermarketCode(),
                template.name(),
                template.description(),
                MealType.valueOf(template.mealType()),
                template.instructions(),
                template.preparationMinutes(),
                template.servings(),
                true,
                template.imageUrl(),
                ingredients
        ));
    }

    private Candidate toCandidate(
            MealTemplateResponse original,
            MealTemplateResponse mandatoryOnly,
            int requestedServings,
            Map<UUID, ProductEntity> products
    ) {
        var multiplier = BigDecimal.valueOf(requestedServings);
        var nutrition = multiply(mandatoryOnly.nutritionPerServing(), multiplier);
        var cost = mandatoryOnly.consumedCostPerServing().multiply(multiplier);
        var ingredientMultiplier = BigDecimal.valueOf(requestedServings)
                .divide(BigDecimal.valueOf(original.servings()), 12, RoundingMode.HALF_UP);
        var ingredients = original.ingredients().stream()
                .filter(ingredient -> !ingredient.optional())
                .map(ingredient -> {
                    var product = products.get(ingredient.productId());
                    var scaledCost = ingredient.calculatedConsumedCost() == null
                            ? null
                            : ingredient.calculatedConsumedCost()
                                    .multiply(ingredientMultiplier)
                                    .setScale(2, RoundingMode.HALF_UP);
                    return new GeneratedMealPlanResult.IngredientSummary(
                            ingredient.productId(),
                            ingredient.productName(),
                            product == null ? ingredient.brand() : product.getBrand(),
                            product == null ? null : product.getCategory().getId(),
                            product == null ? ingredient.category() : product.getCategory().getName(),
                            ingredient.quantity().multiply(ingredientMultiplier)
                                    .setScale(3, RoundingMode.HALF_UP)
                                    .stripTrailingZeros(),
                            ingredient.quantityUnit(),
                            product == null ? null : product.getMeasurementType().name(),
                            product == null ? null : product.getPackageQuantity(),
                            product == null ? null : product.getPackageUnit().name(),
                            product == null ? null : product.getCurrentPrice(),
                            product == null ? null : product.getUnitPrice(),
                            product != null && product.isAvailable(),
                            scaledCost,
                            product != null && ingredient.costCalculationComplete(),
                            ingredient.warnings(),
                            "MEAL_TOTAL"
                    );
                })
                .toList();
        return new Candidate(
                original.id(),
                original.name(),
                MealType.valueOf(original.mealType()),
                original.preparationMinutes(),
                requestedServings,
                ingredients,
                nutrition,
                cost.setScale(2, RoundingMode.HALF_UP),
                mandatoryOnly.calculationComplete(),
                mandatoryOnly.warnings()
        );
    }

    private List<Slot> buildSlots(
            GenerateMealPlanCommand command,
            Map<MealType, List<Candidate>> byType,
            List<String> adaptedTypes
    ) {
        var base = DEFAULT_DISTRIBUTIONS.get(command.mealsPerDay());
        var allowed = command.allowedMealTypes().stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();
        var dayPattern = new ArrayList<MealType>();
        for (int position = 0; position < base.size(); position++) {
            var requested = base.get(position);
            if (command.allowedMealTypes().contains(requested)) {
                dayPattern.add(requested);
                continue;
            }
            var replacement = allowed.stream()
                    .filter(type -> !byType.getOrDefault(type, List.of()).isEmpty())
                    .skip(position % Math.max(1, allowed.size()))
                    .findFirst()
                    .orElseGet(() -> allowed.stream()
                            .filter(type -> !byType.getOrDefault(type, List.of()).isEmpty())
                            .findFirst()
                            .orElseThrow(() -> impossible(
                                    "Allowed meal types have no candidates",
                                    byType,
                                    Map.of(),
                                    command
                            )));
            dayPattern.add(replacement);
            adaptedTypes.add("Position " + (position + 1) + " changed from "
                    + requested + " to " + replacement + " because of allowedMealTypes");
        }
        var slots = new ArrayList<Slot>();
        for (int day = 0; day < command.numberOfDays(); day++) {
            for (int position = 0; position < dayPattern.size(); position++) {
                slots.add(new Slot(day, position, dayPattern.get(position)));
            }
        }
        return List.copyOf(slots);
    }

    private Comparator<Candidate> candidateComparator(
            GenerateMealPlanCommand command,
            Slot slot,
            int slotIndex,
            long seed
    ) {
        return Comparator.comparing((Candidate candidate) -> individualPenalty(command, candidate))
                .thenComparing(
                        candidate -> tieKey(seed, slotIndex, candidate.templateId()),
                        Long::compareUnsigned
                )
                .thenComparing(Candidate::templateId);
    }

    private BigDecimal partialPenalty(
            GenerateMealPlanCommand command,
            List<Selection> selections
    ) {
        var penalty = selections.stream()
                .map(selection -> individualPenalty(command, selection.candidate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var counts = new HashMap<UUID, Integer>();
        for (int index = 0; index < selections.size(); index++) {
            var selection = selections.get(index);
            var count = counts.merge(selection.candidate().templateId(), 1, Integer::sum);
            if (count > command.effectiveMaximumTemplateRepetitions()) {
                penalty = penalty.add(properties.getBeamExcessRepetitionPenalty());
            }
            for (int previous = 0; previous < index; previous++) {
                var previousSelection = selections.get(previous);
                if (!previousSelection.candidate().templateId()
                        .equals(selection.candidate().templateId())) {
                    continue;
                }
                if (previousSelection.slot().dayIndex() == selection.slot().dayIndex()) {
                    penalty = penalty.add(properties.getBeamSameDayRepetitionPenalty());
                } else if (previousSelection.slot().dayIndex() + 1
                        == selection.slot().dayIndex()
                        && previousSelection.slot().position() == selection.slot().position()) {
                    penalty = penalty.add(properties.getBeamConsecutiveRepetitionPenalty());
                }
            }
        }
        return penalty;
    }

    private BigDecimal individualPenalty(
            GenerateMealPlanCommand command,
            Candidate candidate
    ) {
        var targetCalories = divide(
                command.dailyCaloriesTarget(),
                BigDecimal.valueOf(command.mealsPerDay())
        );
        var targetProtein = divide(
                command.dailyProteinTarget(),
                BigDecimal.valueOf(command.mealsPerDay())
        );
        var caloriePenalty = percentage(
                candidate.nutrition().calories().subtract(targetCalories).abs(),
                targetCalories
        ).multiply(properties.getCandidateCaloriePenaltyFactor());
        var proteinPenalty = candidate.nutrition().protein().compareTo(targetProtein) >= 0
                ? BigDecimal.ZERO
                : percentage(
                        targetProtein.subtract(candidate.nutrition().protein()),
                        targetProtein
                ).multiply(properties.getCandidateProteinPenaltyFactor());
        var budgetPenalty = BigDecimal.ZERO;
        if (command.weeklyBudget() != null) {
            var targetCost = divide(
                    command.weeklyBudget(),
                    BigDecimal.valueOf(command.numberOfDays() * command.mealsPerDay())
            );
            if (candidate.cost().compareTo(targetCost) > 0) {
                budgetPenalty = percentage(candidate.cost().subtract(targetCost), targetCost)
                        .multiply(properties.getCandidateBudgetPenaltyFactor());
            }
        }
        var incompletePenalty = candidate.complete()
                ? BigDecimal.ZERO
                : properties.getCandidateIncompletePenalty();
        return caloriePenalty.add(proteinPenalty).add(budgetPenalty)
                .add(BigDecimal.valueOf(candidate.preparationMinutes())
                        .multiply(properties.getCandidatePreparationMinutePenalty()))
                .add(incompletePenalty);
    }

    private GeneratedMealPlanResult buildResult(
            GenerateMealPlanCommand command,
            String supermarketName,
            FinalState best,
            String token,
            Map<String, Integer> rejected,
            List<String> adaptedTypes,
            int candidatesEvaluated,
            int completePlansEvaluated,
            long duration,
            OffsetDateTime generatedAt,
            boolean purchaseAware
    ) {
        var selectionsByDay = best.state().selections().stream()
                .collect(Collectors.groupingBy(
                        selection -> selection.slot().dayIndex(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        var warnings = new ArrayList<GeneratedMealPlanResult.PlanWarning>();
        adaptedTypes.forEach(message -> warnings.add(new GeneratedMealPlanResult.PlanWarning(
                "MEAL_TYPE_ADAPTED",
                message,
                WarningSeverity.INFO,
                null
        )));
        warnings.add(new GeneratedMealPlanResult.PlanWarning(
                purchaseAware ? "PURCHASE_AWARE_OPTIMIZATION" : "CONSUMED_COST_ONLY",
                purchaseAware
                        ? "Budget and optimization use the estimated cost of complete packages"
                        : "Budget uses proportional consumed cost, not the cost of complete packages",
                WarningSeverity.INFO,
                null
        ));
        warnings.add(new GeneratedMealPlanResult.PlanWarning(
                "OPTIONAL_INGREDIENTS_OMITTED",
                "Optional ingredients were not included in generated meals",
                WarningSeverity.INFO,
                null
        ));
        var days = new ArrayList<GeneratedMealPlanResult.DayResult>();
        var weeklyNutrition = zeroNutrition();
        var weeklyCost = BigDecimal.ZERO;
        var constraintsNotMet = new LinkedHashSet<String>();
        for (int dayIndex = 0; dayIndex < command.numberOfDays(); dayIndex++) {
            var selections = selectionsByDay.getOrDefault(dayIndex, List.of());
            var meals = new ArrayList<GeneratedMealPlanResult.PlannedMealResult>();
            var dayNutrition = zeroNutrition();
            var dayCost = BigDecimal.ZERO;
            var dayWarnings = new ArrayList<GeneratedMealPlanResult.PlanWarning>();
            for (var selection : selections) {
                var candidate = selection.candidate();
                dayNutrition = MealPlanScoringService.addNutrition(
                        dayNutrition,
                        candidate.nutrition()
                );
                dayCost = dayCost.add(candidate.cost());
                var mealPenalty = individualPenalty(command, candidate);
                var mealScore = ONE_HUNDRED.subtract(mealPenalty).max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);
                meals.add(new GeneratedMealPlanResult.PlannedMealResult(
                        selection.slot().position(),
                        selection.slot().mealType().name(),
                        candidate.templateId(),
                        candidate.name(),
                        candidate.servings(),
                        candidate.preparationMinutes(),
                        candidate.ingredients(),
                        scaleNutrition(candidate.nutrition()),
                        money(candidate.cost()),
                        mealScore,
                        candidate.complete(),
                        candidate.warnings(),
                        null,
                        false,
                        com.sean.supermarketmealplanner.mealplan.domain.MealSelectionSource.GENERATED,
                        0,
                        null,
                        null,
                        null
                ));
                if (!candidate.complete()) {
                    var warning = new GeneratedMealPlanResult.PlanWarning(
                            "INCOMPLETE_CALCULATION",
                            candidate.name() + " has an incomplete calculation",
                            WarningSeverity.WARNING,
                            dayIndex
                    );
                    warnings.add(warning);
                    dayWarnings.add(warning);
                    constraintsNotMet.add("Some selected meals have incomplete calculations");
                }
            }
            var calorieDeviation = dayNutrition.calories()
                    .subtract(command.dailyCaloriesTarget()).abs();
            var calorieDeviationPercentage = percentage(
                    calorieDeviation,
                    command.dailyCaloriesTarget()
            );
            var proteinDeviation = dayNutrition.protein()
                    .subtract(command.dailyProteinTarget());
            if (calorieDeviationPercentage.compareTo(
                    properties.getWarningCalorieMarginPercentage()
            ) > 0) {
                var warning = new GeneratedMealPlanResult.PlanWarning(
                        "CALORIE_DEVIATION",
                        "Day " + (dayIndex + 1) + " differs from the calorie target by "
                                + calorieDeviationPercentage.setScale(1, RoundingMode.HALF_UP) + "%",
                        WarningSeverity.WARNING,
                        dayIndex
                );
                warnings.add(warning);
                dayWarnings.add(warning);
                constraintsNotMet.add("Daily calorie target outside the configured margin");
            }
            if (proteinDeviation.signum() < 0) {
                var warning = new GeneratedMealPlanResult.PlanWarning(
                        "PROTEIN_DEFICIT",
                        "Day " + (dayIndex + 1) + " is below the protein target by "
                                + proteinDeviation.abs().setScale(1, RoundingMode.HALF_UP) + " g",
                        WarningSeverity.WARNING,
                        dayIndex
                );
                warnings.add(warning);
                dayWarnings.add(warning);
                constraintsNotMet.add("Daily protein minimum not reached");
            }
            days.add(new GeneratedMealPlanResult.DayResult(
                    dayIndex,
                    command.startDate().plusDays(dayIndex),
                    List.copyOf(meals),
                    scaleNutrition(dayNutrition),
                    money(dayCost),
                    command.dailyCaloriesTarget(),
                    command.dailyProteinTarget(),
                    nutrient(calorieDeviation),
                    calorieDeviationPercentage.setScale(2, RoundingMode.HALF_UP),
                    nutrient(proteinDeviation),
                    best.score().dailyScores().getOrDefault(dayIndex, BigDecimal.ZERO),
                    List.copyOf(dayWarnings),
                    null
            ));
            weeklyNutrition = MealPlanScoringService.addNutrition(weeklyNutrition, dayNutrition);
            weeklyCost = weeklyCost.add(dayCost);
        }
        var budgetDifference = command.weeklyBudget() == null
                ? null
                : command.weeklyBudget().subtract(weeklyCost);
        var budgetExceeded = budgetDifference != null && budgetDifference.signum() < 0;
        var budgetDeviation = command.weeklyBudget() == null
                ? null
                : percentage(weeklyCost.subtract(command.weeklyBudget()).abs(), command.weeklyBudget())
                .setScale(2, RoundingMode.HALF_UP);
        var purchaseCalculation = purchaseAware
                ? best.state().purchaseState().calculation()
                : null;
        var effectiveBudgetExceeded = purchaseAware
                ? purchaseCalculation.purchaseBudgetExceeded()
                : budgetExceeded;
        var effectiveBudgetDifference = purchaseAware
                ? purchaseCalculation.purchaseBudgetDifference()
                : budgetDifference;
        if (effectiveBudgetExceeded) {
            warnings.add(new GeneratedMealPlanResult.PlanWarning(
                    "BUDGET_EXCEEDED",
                    (purchaseAware ? "Purchase cost" : "Consumed cost")
                            + " exceeds the weekly budget by "
                            + effectiveBudgetDifference.abs().setScale(2, RoundingMode.HALF_UP),
                    WarningSeverity.WARNING,
                    null
            ));
            constraintsNotMet.add(purchaseAware
                    ? "Weekly purchase-cost budget exceeded"
                    : "Weekly consumed-cost budget exceeded");
        }
        if (best.score().maximumObservedRepetition()
                > command.effectiveMaximumTemplateRepetitions()) {
            warnings.add(new GeneratedMealPlanResult.PlanWarning(
                    "REPETITION_LIMIT_EXCEEDED",
                    "The best viable plan exceeds the preferred template repetition limit",
                    WarningSeverity.WARNING,
                    null
            ));
            constraintsNotMet.add("Preferred template repetition limit exceeded");
        }
        var breakdown = new GeneratedMealPlanResult.ScoreBreakdown(
                best.score().calorieScore(),
                best.score().proteinScore(),
                best.score().budgetScore(),
                best.score().varietyScore(),
                best.score().repetitionScore(),
                best.score().completenessScore(),
                best.score().preparationScore(),
                purchaseAware ? best.purchaseScore().purchaseCostScore() : null,
                purchaseAware ? best.purchaseScore().consumedCostScore() : null,
                purchaseAware ? best.purchaseScore().purchaseBudgetScore() : null,
                purchaseAware ? best.purchaseScore().wasteCostScore() : null,
                purchaseAware ? best.purchaseScore().wastePercentageScore() : null,
                purchaseAware ? best.purchaseScore().usefulReuseScore() : null,
                purchaseAware ? best.purchaseScore().uniqueProductsScore() : null,
                purchaseAware ? best.purchaseScore().packageCountScore() : null,
                best.totalScore()
        );
        var variety = new GeneratedMealPlanResult.VarietyMetrics(
                best.score().uniqueTemplates(),
                best.score().repeatedTemplates(),
                best.score().maximumObservedRepetition(),
                best.score().varietyMetricScore()
        );
        return new GeneratedMealPlanResult(
                false,
                null,
                token,
                command.name().trim(),
                command.supermarketCode().toUpperCase(Locale.ROOT),
                supermarketName,
                command.startDate(),
                command.numberOfDays(),
                command.mealsPerDay(),
                command.servings(),
                command.deterministicSeed(),
                purchaseAware
                        ? GenerationStrategy.PURCHASE_AWARE_SCORING
                        : GenerationStrategy.SCORING,
                MealPlanStatus.DRAFT,
                criteria(command),
                List.copyOf(days),
                scaleNutrition(weeklyNutrition),
                money(weeklyCost),
                purchaseAware
                        ? purchaseMetrics(
                                purchaseCalculation,
                                best.state().economicallyUsefulReuseCount(),
                                best
                        )
                        : null,
                command.weeklyBudget(),
                budgetDifference == null ? null : money(budgetDifference),
                budgetExceeded,
                budgetDeviation,
                best.totalScore(),
                breakdown,
                variety,
                best.state().selections().stream()
                        .allMatch(selection -> selection.candidate().complete())
                        && (!purchaseAware || purchaseCalculation.calculationComplete()),
                List.copyOf(warnings),
                constraintsApplied(command),
                List.copyOf(constraintsNotMet),
                Map.copyOf(rejected),
                new GeneratedMealPlanResult.GenerationMetadata(
                        purchaseAware
                                ? GenerationStrategy.PURCHASE_AWARE_SCORING
                                : GenerationStrategy.SCORING,
                        command.deterministicSeed(),
                        duration,
                        candidatesEvaluated,
                        completePlansEvaluated,
                        generatedAt,
                        purchaseAware ? PURCHASE_AWARE_ALGORITHM_VERSION : ALGORITHM_VERSION,
                        properties.getBeamWidth(),
                        properties.getCandidatesPerPosition(),
                        purchaseAware ? command.optimizationPreset() : null,
                        purchaseAware ? best.purchaseScore().weights() : Map.of()
                ),
                null,
                null,
                0,
                0,
                com.sean.supermarketmealplanner.mealplan.domain.ShoppingListFreshness.NONE,
                null,
                false,
                null
        );
    }

    private GeneratedMealPlanResult.GenerationCriteria criteria(
            GenerateMealPlanCommand command
    ) {
        return new GeneratedMealPlanResult.GenerationCriteria(
                command.dailyCaloriesTarget(),
                command.dailyProteinTarget(),
                command.allowedMealTypes().stream().map(Enum::name).collect(Collectors.toSet()),
                command.requiredDietaryTags(),
                command.excludedAllergens(),
                command.excludedTemplateIds(),
                command.excludedProductIds(),
                command.maximumPreparationMinutes(),
                command.effectiveMaximumTemplateRepetitions(),
                command.varietyPreference(),
                command.allowIncompleteCalculations()
        );
    }

    private GeneratedMealPlanResult.PurchaseMetrics purchaseMetrics(
            PurchaseMetricsCalculator.PurchaseCalculation purchase,
            int economicallyUsefulReuse,
            FinalState best
    ) {
        var reasons = new ArrayList<String>();
        if (economicallyUsefulReuse > 0) {
            reasons.add("Reutiliza ingredientes cuando evita envases o aprovecha sobrantes");
        }
        if (purchase.wastePercentage().compareTo(new BigDecimal("30")) <= 0) {
            reasons.add("Mantiene el desperdicio estimado por debajo del 30 %");
        }
        if (purchase.weeklyBudget() != null && !purchase.purchaseBudgetExceeded()) {
            reasons.add("El coste estimado de compra entra en el presupuesto");
        }
        if (best.score().calorieScore().compareTo(new BigDecimal("80")) >= 0
                && best.score().proteinScore().compareTo(new BigDecimal("80")) >= 0) {
            reasons.add("Mantiene un buen ajuste de calorías y proteína");
        }
        if (reasons.isEmpty()) {
            reasons.add("Es la mejor solución viable con las restricciones solicitadas");
        }
        return new GeneratedMealPlanResult.PurchaseMetrics(
                purchase.totalConsumedCost(),
                purchase.totalPurchaseCost(),
                purchase.totalWasteCost(),
                purchase.wastePercentage(),
                purchase.totalPackages(),
                purchase.uniqueProductCount(),
                purchase.reusedProductCount(),
                economicallyUsefulReuse,
                purchase.purchaseBudgetDifference(),
                purchase.purchaseBudgetExceeded(),
                purchase.purchaseBudgetDeviationPercentage(),
                purchase.calculationComplete(),
                purchase.warnings().stream()
                        .map(PurchaseMetricsCalculator.PurchaseWarning::message)
                        .distinct()
                        .toList(),
                List.copyOf(reasons)
        );
    }

    private List<String> constraintsApplied(GenerateMealPlanCommand command) {
        var result = new ArrayList<String>();
        result.add("Supermarket: " + command.supermarketCode().toUpperCase(Locale.ROOT));
        result.add("Allowed meal types: " + command.allowedMealTypes());
        result.add("Required dietary tags: " + command.requiredDietaryTags());
        result.add("Excluded allergens: " + command.excludedAllergens());
        result.add("Maximum preparation minutes: " + command.maximumPreparationMinutes());
        result.add("Maximum preferred repetitions: "
                + command.effectiveMaximumTemplateRepetitions());
        result.add("Incomplete calculations allowed: "
                + command.allowIncompleteCalculations());
        return List.copyOf(result);
    }

    private MealPlanGenerationException impossible(
            String message,
            Map<MealType, List<Candidate>> byType,
            Map<String, Integer> rejected,
            GenerateMealPlanCommand command
    ) {
        var counts = new LinkedHashMap<String, Integer>();
        for (var type : MealType.values()) {
            counts.put(type.name(), byType.getOrDefault(type, List.of()).size());
        }
        var conflicts = List.of(
                "allowedMealTypes=" + command.allowedMealTypes(),
                "requiredDietaryTags=" + command.requiredDietaryTags(),
                "excludedAllergens=" + command.excludedAllergens(),
                "maximumPreparationMinutes=" + command.maximumPreparationMinutes(),
                "allowIncompleteCalculations=" + command.allowIncompleteCalculations()
        );
        return new MealPlanGenerationException(
                message,
                counts,
                rejected,
                conflicts,
                List.of(
                        "Increase maximumPreparationMinutes",
                        "Allow incomplete calculations",
                        "Remove a required dietary tag",
                        "Allow another meal type",
                        "Reduce mealsPerDay"
                )
        );
    }

    private SupermarketCode parseSupermarket(String raw) {
        try {
            return SupermarketCode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new MealPlanValidationException("Invalid supermarketCode: " + raw);
        }
    }

    private String generationToken(
            GenerateMealPlanCommand command,
            List<Candidate> candidates
    ) {
        var canonical = new StringBuilder()
                .append(command.supermarketCode().toUpperCase(Locale.ROOT)).append('|')
                .append(command.name().trim()).append('|')
                .append(command.startDate()).append('|')
                .append(command.numberOfDays()).append('|')
                .append(command.mealsPerDay()).append('|')
                .append(command.servings()).append('|')
                .append(command.dailyCaloriesTarget().stripTrailingZeros()).append('|')
                .append(command.dailyProteinTarget().stripTrailingZeros()).append('|')
                .append(command.weeklyBudget()).append('|')
                .append(command.allowedMealTypes().stream().map(Enum::name).sorted().toList())
                .append('|').append(command.requiredDietaryTags().stream().sorted().toList())
                .append('|').append(command.excludedAllergens().stream().sorted().toList())
                .append('|').append(command.excludedTemplateIds().stream().sorted().toList())
                .append('|').append(command.excludedProductIds().stream().sorted().toList())
                .append('|').append(command.maximumPreparationMinutes())
                .append('|').append(command.effectiveMaximumTemplateRepetitions())
                .append('|').append(command.varietyPreference())
                .append('|').append(command.allowIncompleteCalculations())
                .append('|').append(command.deterministicSeed());
        if (command.strategy() == GenerationStrategy.PURCHASE_AWARE_SCORING) {
            canonical.append('|').append(command.strategy())
                    .append('|').append(command.optimizationPreset())
                    .append('|').append(PURCHASE_AWARE_ALGORITHM_VERSION)
                    .append('|').append(purchaseScoringService.weights(command));
        }
        candidates.stream().sorted(Comparator.comparing(Candidate::templateId)).forEach(candidate ->
                canonical.append('|').append(candidate.templateId())
                        .append(':').append(candidate.nutrition())
                        .append(':').append(candidate.cost())
                        .append(':').append(candidate.ingredients())
                        .append(':').append(candidate.complete())
        );
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    static long tieKey(long seed, int position, UUID templateId) {
        return mix64(seed ^ templateId.getMostSignificantBits()
                ^ templateId.getLeastSignificantBits()
                ^ ((long) position * 0x9E3779B97F4A7C15L));
    }

    private long sequenceKey(long seed, List<Selection> selections) {
        var value = seed;
        for (var selection : selections) {
            value = mix64(value ^ selection.candidate().templateId().getMostSignificantBits()
                    ^ selection.candidate().templateId().getLeastSignificantBits()
                    ^ selection.slot().dayIndex()
                    ^ ((long) selection.slot().position() << 32));
        }
        return value;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private List<MealPlanScoringService.ScoredMeal> toScoredMeals(
            List<Selection> selections
    ) {
        return selections.stream().map(selection -> new MealPlanScoringService.ScoredMeal(
                selection.slot().dayIndex(),
                selection.slot().position(),
                selection.candidate().templateId(),
                selection.slot().mealType(),
                selection.candidate().ingredients().stream()
                        .map(GeneratedMealPlanResult.IngredientSummary::productId)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                selection.candidate().nutrition(),
                selection.candidate().cost(),
                selection.candidate().preparationMinutes(),
                selection.candidate().complete()
        )).toList();
    }

    private PurchaseMetricsCalculator.IngredientInput purchaseInput(
            GeneratedMealPlanResult.IngredientSummary ingredient
    ) {
        return new PurchaseMetricsCalculator.IngredientInput(
                ingredient.productId(),
                ingredient.productName(),
                ingredient.brand(),
                ingredient.categoryId(),
                ingredient.categoryName(),
                ingredient.quantity(),
                ingredient.quantityUnit(),
                ingredient.measurementType(),
                ingredient.packageQuantity(),
                ingredient.packageUnit(),
                ingredient.packagePrice(),
                ingredient.unitPrice(),
                ingredient.available(),
                ingredient.warnings()
        );
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal base) {
        if (base.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(ONE_HUNDRED).divide(base, 12, RoundingMode.HALF_UP);
    }

    private BigDecimal divide(BigDecimal amount, BigDecimal divisor) {
        if (divisor.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(divisor, 12, RoundingMode.HALF_UP);
    }

    private NutritionBreakdown multiply(NutritionBreakdown value, BigDecimal multiplier) {
        return new NutritionBreakdown(
                value.calories().multiply(multiplier),
                value.protein().multiply(multiplier),
                value.carbohydrates().multiply(multiplier),
                value.fat().multiply(multiplier),
                value.fiber().multiply(multiplier),
                value.sugar().multiply(multiplier),
                value.salt().multiply(multiplier)
        );
    }

    private NutritionBreakdown zeroNutrition() {
        return new NutritionBreakdown(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    private NutritionBreakdown scaleNutrition(NutritionBreakdown value) {
        return new NutritionBreakdown(
                nutrient(value.calories()),
                nutrient(value.protein()),
                nutrient(value.carbohydrates()),
                nutrient(value.fat()),
                nutrient(value.fiber()),
                nutrient(value.sugar()),
                nutrient(value.salt())
        );
    }

    private BigDecimal nutrient(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void increment(Map<String, Integer> values, String key) {
        values.merge(key, 1, Integer::sum);
    }

    private record Candidate(
            UUID templateId,
            String name,
            MealType mealType,
            int preparationMinutes,
            int servings,
            List<GeneratedMealPlanResult.IngredientSummary> ingredients,
            NutritionBreakdown nutrition,
            BigDecimal cost,
            boolean complete,
            List<String> warnings
    ) {
    }

    private record Slot(int dayIndex, int position, MealType mealType) {
    }

    private record Selection(Slot slot, Candidate candidate) {
    }

    private record BeamState(
            List<Selection> selections,
            BigDecimal penalty,
            long tieKey,
            PurchaseMetricsCalculator.PurchaseState purchaseState,
            int economicallyUsefulReuseCount
    ) {
    }

    private record FinalState(
            BeamState state,
            MealPlanScoringService.ScoreOutput score,
            PurchaseAwareMealPlanScoringService.ScoreOutput purchaseScore
    ) {
        BigDecimal totalScore() {
            return purchaseScore == null ? score.totalScore() : purchaseScore.totalScore();
        }
    }
}
