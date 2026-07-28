package com.sean.supermarketmealplanner.mealtemplate.application;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductRepository;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateEntity;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateRepository;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketEntity;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MealTemplateService {

    private final MealTemplateRepository mealTemplateRepository;
    private final ProductRepository productRepository;
    private final SupermarketRepository supermarketRepository;
    private final MealTemplateCalculationService calculationService;

    public MealTemplateService(
            MealTemplateRepository mealTemplateRepository,
            ProductRepository productRepository,
            SupermarketRepository supermarketRepository,
            MealTemplateCalculationService calculationService
    ) {
        this.mealTemplateRepository = mealTemplateRepository;
        this.productRepository = productRepository;
        this.supermarketRepository = supermarketRepository;
        this.calculationService = calculationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<MealTemplateResponse> findAll(MealTemplateSearchCriteria criteria) {
        var evaluations = mealTemplateRepository.findAllByArchivedFalse().stream()
                .filter(template -> matchesStoredFields(template, criteria))
                .map(calculationService::evaluate)
                .filter(evaluation -> matchesCalculatedFields(evaluation, criteria))
                .map(evaluation -> addOptionalAllergenWarning(evaluation, criteria))
                .sorted(comparator(criteria))
                .toList();

        var total = evaluations.size();
        var start = Math.min(criteria.page() * criteria.size(), total);
        var end = Math.min(start + criteria.size(), total);
        var content = evaluations.subList(start, end).stream()
                .map(MealTemplateCalculationService.Evaluation::response)
                .toList();
        var totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / criteria.size());
        return new PageResponse<>(
                content,
                criteria.page(),
                criteria.size(),
                total,
                totalPages,
                criteria.page() == 0,
                criteria.page() + 1 >= totalPages
        );
    }

    @Transactional(readOnly = true)
    public MealTemplateResponse findById(UUID id) {
        return calculationService.evaluate(findEntity(id)).response();
    }

    @Transactional
    public MealTemplateResponse create(MealTemplateRequest request) {
        var supermarket = resolveSupermarket(request.supermarketCode());
        validateNameUnique(supermarket, request.name(), null);
        var template = new MealTemplateEntity(supermarket, false);
        applyRequest(template, request);
        return calculationService.evaluate(mealTemplateRepository.saveAndFlush(template)).response();
    }

    @Transactional
    public MealTemplateResponse update(UUID id, MealTemplateRequest request) {
        var template = findEntity(id);
        var requestedSupermarket = resolveSupermarket(request.supermarketCode());
        if (!template.getSupermarket().getId().equals(requestedSupermarket.getId())) {
            throw new MealTemplateValidationException(
                    "A meal template cannot be moved to another supermarket"
            );
        }
        validateNameUnique(requestedSupermarket, request.name(), id);
        template.clearCollections();
        mealTemplateRepository.flush();
        applyRequest(template, request);
        return calculationService.evaluate(mealTemplateRepository.saveAndFlush(template)).response();
    }

    @Transactional
    public MealTemplateResponse changeStatus(UUID id, boolean active) {
        var template = findEntity(id);
        template.changeActive(active);
        return calculationService.evaluate(mealTemplateRepository.saveAndFlush(template)).response();
    }

    @Transactional
    public void archive(UUID id) {
        var template = findEntity(id);
        template.archive();
        mealTemplateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public MealTemplateResponse preview(MealTemplateRequest request) {
        var supermarket = resolveSupermarket(request.supermarketCode());
        var transientTemplate = new MealTemplateEntity(supermarket, false);
        applyRequest(transientTemplate, request);
        return calculationService.evaluate(transientTemplate).response();
    }

    private void applyRequest(MealTemplateEntity template, MealTemplateRequest request) {
        validateRequestRules(request);
        var products = resolveProducts(request.ingredients());
        validateIngredientRules(template.getSupermarket(), request.ingredients(), products);
        template.update(
                request.name().trim(),
                request.description().trim(),
                request.mealType(),
                request.instructions().stream().map(String::trim).toList(),
                request.preparationMinutes(),
                request.servings(),
                request.active(),
                blankToNull(request.imageUrl())
        );
        template.replaceIngredients(request.ingredients().stream()
                .map(ingredient -> new MealTemplateEntity.IngredientData(
                        products.get(ingredient.productId()),
                        ingredient.quantity(),
                        ingredient.quantityUnit(),
                        ingredient.optional(),
                        ingredient.sortOrder(),
                        blankToNull(ingredient.notes())
                ))
                .toList());
    }

    private void validateRequestRules(MealTemplateRequest request) {
        if (request.servings() <= 0) {
            throw new MealTemplateValidationException("servings must be greater than zero");
        }
        if (request.preparationMinutes() < 0) {
            throw new MealTemplateValidationException(
                    "preparationMinutes must be greater than or equal to zero"
            );
        }
        if (request.ingredients() == null || request.ingredients().isEmpty()) {
            throw new MealTemplateValidationException(
                    "A meal template must contain at least one ingredient"
            );
        }
        if (request.ingredients().stream().noneMatch(ingredient -> !ingredient.optional())) {
            throw new MealTemplateValidationException(
                    "A meal template must contain at least one required ingredient"
            );
        }
        if (request.instructions() == null || request.instructions().isEmpty()) {
            throw new MealTemplateValidationException(
                    "A meal template must contain at least one instruction"
            );
        }
    }

    private Map<UUID, ProductEntity> resolveProducts(
            List<MealTemplateIngredientRequest> ingredients
    ) {
        var ids = ingredients.stream()
                .map(MealTemplateIngredientRequest::productId)
                .collect(Collectors.toCollection(HashSet::new));
        var products = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        var missing = new HashSet<>(ids);
        missing.removeAll(products.keySet());
        if (!missing.isEmpty()) {
            throw new MealTemplateValidationException(
                    "Product not found: " + missing.iterator().next()
            );
        }
        return products;
    }

    private void validateIngredientRules(
            SupermarketEntity supermarket,
            List<MealTemplateIngredientRequest> ingredients,
            Map<UUID, ProductEntity> products
    ) {
        var productIds = new HashSet<UUID>();
        var sortOrders = new HashSet<Integer>();
        for (var ingredient : ingredients) {
            if (!productIds.add(ingredient.productId())) {
                throw new MealTemplateValidationException(
                        "A product cannot be repeated in a meal template: "
                                + ingredient.productId()
                );
            }
            if (!sortOrders.add(ingredient.sortOrder())) {
                throw new MealTemplateValidationException(
                        "Ingredient sortOrder values must be unique"
                );
            }
            if (ingredient.quantity() == null || ingredient.quantity().signum() <= 0) {
                throw new MealTemplateValidationException(
                        "Ingredient quantity must be greater than zero"
                );
            }
            if (ingredient.sortOrder() < 0) {
                throw new MealTemplateValidationException(
                        "Ingredient sortOrder must be greater than or equal to zero"
                );
            }
            var product = products.get(ingredient.productId());
            if (!product.getSupermarket().getId().equals(supermarket.getId())) {
                throw new MealTemplateValidationException(
                        "Product " + product.getId() + " belongs to another supermarket"
                );
            }
            if (!ingredient.quantityUnit().isCompatibleWith(product.getMeasurementType())) {
                throw new MealTemplateValidationException(
                        "Unit " + ingredient.quantityUnit() + " is incompatible with product "
                                + product.getName() + " (" + product.getMeasurementType() + ")"
                );
            }
        }
    }

    private boolean matchesStoredFields(
            MealTemplateEntity template,
            MealTemplateSearchCriteria criteria
    ) {
        if (criteria.supermarketCode() != null
                && template.getSupermarket().getCode() != criteria.supermarketCode()) {
            return false;
        }
        if (criteria.mealType() != null && template.getMealType() != criteria.mealType()) {
            return false;
        }
        if (criteria.active() != null && template.isActive() != criteria.active()) {
            return false;
        }
        if (criteria.maximumPreparationMinutes() != null
                && template.getPreparationMinutes() > criteria.maximumPreparationMinutes()) {
            return false;
        }
        if (criteria.query() != null) {
            var query = criteria.query().toLowerCase(Locale.ROOT);
            return template.getName().toLowerCase(Locale.ROOT).contains(query)
                    || template.getDescription().toLowerCase(Locale.ROOT).contains(query);
        }
        return true;
    }

    private boolean matchesCalculatedFields(
            MealTemplateCalculationService.Evaluation evaluation,
            MealTemplateSearchCriteria criteria
    ) {
        var response = evaluation.response();
        if (criteria.minimumProtein() != null
                && (!response.nutritionComplete()
                || response.nutritionPerServing().protein()
                .compareTo(criteria.minimumProtein()) < 0)) {
            return false;
        }
        if (criteria.maximumCalories() != null
                && (!response.nutritionComplete()
                || response.nutritionPerServing().calories()
                .compareTo(criteria.maximumCalories()) > 0)) {
            return false;
        }
        if (!java.util.Collections.disjoint(
                evaluation.mandatoryAllergens(),
                criteria.excludedAllergens()
        )) {
            return false;
        }
        return criteria.dietaryTags().stream()
                .allMatch(tag -> matchesDietaryTag(tag, evaluation));
    }

    private boolean matchesDietaryTag(
            String tag,
            MealTemplateCalculationService.Evaluation evaluation
    ) {
        return switch (tag) {
            case "VEGAN", "VEGETARIAN" -> evaluation.mandatoryProductTagSets().stream()
                    .allMatch(tags -> tags.contains(tag));
            case "GLUTEN_FREE" -> !evaluation.mandatoryAllergens().contains("GLUTEN");
            case "LACTOSE_FREE" -> !evaluation.mandatoryAllergens().contains("MILK");
            case "HIGH_PROTEIN" -> evaluation.response().nutritionComplete()
                    && evaluation.response().nutritionPerServing().protein()
                    .compareTo(MealTemplateCalculationService.HIGH_PROTEIN_THRESHOLD) >= 0;
            case "LOW_CALORIE" -> evaluation.response().nutritionComplete()
                    && evaluation.response().nutritionPerServing().calories()
                    .compareTo(MealTemplateCalculationService.LOW_CALORIE_THRESHOLD) <= 0;
            case "HIGH_FIBER" -> evaluation.response().nutritionComplete()
                    && evaluation.response().nutritionPerServing().fiber()
                    .compareTo(MealTemplateCalculationService.HIGH_FIBER_THRESHOLD) >= 0;
            default -> false;
        };
    }

    private MealTemplateCalculationService.Evaluation addOptionalAllergenWarning(
            MealTemplateCalculationService.Evaluation evaluation,
            MealTemplateSearchCriteria criteria
    ) {
        var optionalMatches = new HashSet<>(evaluation.optionalAllergens());
        optionalMatches.retainAll(criteria.excludedAllergens());
        if (optionalMatches.isEmpty()) {
            return evaluation;
        }
        var response = evaluation.response().withWarnings(List.of(
                "Un ingrediente opcional contiene alérgenos excluidos: "
                        + String.join(", ", optionalMatches)
        ));
        return new MealTemplateCalculationService.Evaluation(
                response,
                evaluation.mandatoryProductTagSets(),
                evaluation.mandatoryAllergens(),
                evaluation.optionalAllergens()
        );
    }

    private Comparator<MealTemplateCalculationService.Evaluation> comparator(
            MealTemplateSearchCriteria criteria
    ) {
        Comparator<MealTemplateCalculationService.Evaluation> comparator = switch (
                criteria.sortField()
        ) {
            case "preparationMinutes" -> Comparator.comparingInt(
                    value -> value.response().preparationMinutes()
            );
            case "caloriesPerServing" -> Comparator.comparing(
                    value -> value.response().nutritionPerServing().calories()
            );
            case "proteinPerServing" -> Comparator.comparing(
                    value -> value.response().nutritionPerServing().protein()
            );
            case "costPerServing" -> Comparator.comparing(
                    value -> value.response().consumedCostPerServing()
            );
            case "updatedAt" -> Comparator.comparing(value -> value.response().updatedAt());
            default -> Comparator.comparing(
                    value -> value.response().name().toLowerCase(Locale.ROOT)
            );
        };
        if (criteria.descending()) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(value -> value.response().id());
    }

    private MealTemplateEntity findEntity(UUID id) {
        return mealTemplateRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new MealTemplateNotFoundException(id));
    }

    private SupermarketEntity resolveSupermarket(String rawCode) {
        try {
            var code = SupermarketCode.valueOf(rawCode.trim().toUpperCase(Locale.ROOT));
            return supermarketRepository.findByCode(code)
                    .orElseThrow(() -> new MealTemplateValidationException(
                            "Invalid supermarketCode: " + rawCode
                    ));
        } catch (IllegalArgumentException exception) {
            throw new MealTemplateValidationException("Invalid supermarketCode: " + rawCode);
        }
    }

    private void validateNameUnique(
            SupermarketEntity supermarket,
            String name,
            UUID currentTemplateId
    ) {
        mealTemplateRepository.findBySupermarketIdAndNameIgnoreCase(
                supermarket.getId(),
                name.trim()
        ).filter(existing -> !existing.getId().equals(currentTemplateId))
                .ifPresent(existing -> {
                    throw new MealTemplateValidationException(
                            "A meal template with this name already exists"
                    );
                });
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
