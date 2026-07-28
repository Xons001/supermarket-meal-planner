package com.sean.supermarketmealplanner.mealtemplate.application;

import com.sean.supermarketmealplanner.catalog.domain.PackageUnit;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductAllergenRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductDietaryTagRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateEntity;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateIngredientEntity;
import com.sean.supermarketmealplanner.nutrition.infrastructure.persistence.NutritionEntity;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MealTemplateCalculationService {

    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final BigDecimal HIGH_PROTEIN_THRESHOLD = new BigDecimal("20");
    public static final BigDecimal LOW_CALORIE_THRESHOLD = new BigDecimal("400");
    public static final BigDecimal HIGH_FIBER_THRESHOLD = new BigDecimal("6");
    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");

    private final ProductDietaryTagRepository dietaryTagRepository;
    private final ProductAllergenRepository allergenRepository;

    public MealTemplateCalculationService(
            ProductDietaryTagRepository dietaryTagRepository,
            ProductAllergenRepository allergenRepository
    ) {
        this.dietaryTagRepository = dietaryTagRepository;
        this.allergenRepository = allergenRepository;
    }

    public Evaluation evaluate(MealTemplateEntity template) {
        var ingredients = template.getIngredients();
        var productIds = ingredients.stream()
                .map(ingredient -> ingredient.getProduct().getId())
                .toList();
        var tagsByProduct = dietaryTagRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(
                        relation -> relation.getProduct().getId(),
                        Collectors.mapping(
                                relation -> relation.getDietaryTag().getCode(),
                                Collectors.toSet()
                        )
                ));
        var allergensByProduct = allergenRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(
                        relation -> relation.getProduct().getId(),
                        Collectors.mapping(
                                relation -> relation.getAllergen().getCode(),
                                Collectors.toSet()
                        )
                ));

        var ingredientResponses = new ArrayList<MealTemplateIngredientResponse>();
        var templateWarnings = new LinkedHashSet<String>();
        var totals = MutableNutrition.zero();
        var totalCost = BigDecimal.ZERO;
        var nutritionComplete = true;
        var costComplete = true;
        var mandatoryTagSets = new ArrayList<Set<String>>();
        var mandatoryAllergens = new LinkedHashSet<String>();
        var optionalAllergens = new LinkedHashSet<String>();

        for (var ingredient : ingredients) {
            var calculation = calculateIngredient(ingredient);
            ingredientResponses.add(calculation.response());
            templateWarnings.addAll(calculation.response().warnings());
            totals.add(calculation.rawNutrition());
            if (calculation.rawCost() != null) {
                totalCost = totalCost.add(calculation.rawCost(), MATH_CONTEXT);
            }
            nutritionComplete &= calculation.response().nutritionCalculationComplete();
            costComplete &= calculation.response().costCalculationComplete();

            var productId = ingredient.getProduct().getId();
            if (ingredient.isOptional()) {
                optionalAllergens.addAll(allergensByProduct.getOrDefault(productId, Set.of()));
            } else {
                mandatoryTagSets.add(tagsByProduct.getOrDefault(productId, Set.of()));
                mandatoryAllergens.addAll(allergensByProduct.getOrDefault(productId, Set.of()));
            }
        }

        var servings = BigDecimal.valueOf(template.getServings());
        var totalNutrition = totals.rounded();
        var perServingNutrition = totals.dividedBy(servings).rounded();
        var roundedTotalCost = money(totalCost);
        var perServingCost = money(totalCost.divide(servings, MATH_CONTEXT));
        var response = new MealTemplateResponse(
                template.getId(),
                template.getSupermarket().getCode().name(),
                template.getSupermarket().getName(),
                template.getName(),
                template.getDescription(),
                template.getMealType().name(),
                template.getInstructions(),
                template.getPreparationMinutes(),
                template.getServings(),
                template.isActive(),
                template.getImageUrl(),
                List.copyOf(ingredientResponses),
                totalNutrition,
                perServingNutrition,
                roundedTotalCost,
                perServingCost,
                nutritionComplete && costComplete,
                nutritionComplete,
                costComplete,
                List.copyOf(templateWarnings),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                template.isDemoData()
        );
        return new Evaluation(
                response,
                List.copyOf(mandatoryTagSets),
                Set.copyOf(mandatoryAllergens),
                Set.copyOf(optionalAllergens)
        );
    }

    private IngredientCalculation calculateIngredient(MealTemplateIngredientEntity ingredient) {
        var product = ingredient.getProduct();
        var warnings = new ArrayList<String>();
        if (!ingredient.getQuantityUnit().isCompatibleWith(product.getMeasurementType())) {
            warnings.add("La unidad no es compatible con " + product.getName() + ".");
            return incompleteIngredient(ingredient, warnings);
        }
        if (!product.isAvailable()) {
            warnings.add(product.getName() + " figura como no disponible.");
        }

        var nutrition = calculateNutrition(product, ingredient.getQuantity(), warnings);
        var cost = calculateCost(product, ingredient.getQuantity(), warnings);
        var nutritionResponse = nutrition.values() == null ? null : nutrition.values().rounded();
        var roundedCost = cost.value() == null ? null : money(cost.value());
        return new IngredientCalculation(
                new MealTemplateIngredientResponse(
                        product.getId(),
                        product.getName(),
                        product.getBrand(),
                        product.getCategory().getName(),
                        ingredient.getQuantity(),
                        ingredient.getQuantityUnit().name(),
                        ingredient.isOptional(),
                        ingredient.getSortOrder(),
                        ingredient.getNotes(),
                        nutritionResponse,
                        roundedCost,
                        nutrition.complete(),
                        cost.complete(),
                        nutrition.complete() && cost.complete(),
                        List.copyOf(warnings)
                ),
                nutrition.values() == null ? MutableNutrition.zero() : nutrition.values(),
                cost.value()
        );
    }

    private IngredientCalculation incompleteIngredient(
            MealTemplateIngredientEntity ingredient,
            List<String> warnings
    ) {
        var product = ingredient.getProduct();
        return new IngredientCalculation(
                new MealTemplateIngredientResponse(
                        product.getId(),
                        product.getName(),
                        product.getBrand(),
                        product.getCategory().getName(),
                        ingredient.getQuantity(),
                        ingredient.getQuantityUnit().name(),
                        ingredient.isOptional(),
                        ingredient.getSortOrder(),
                        ingredient.getNotes(),
                        null,
                        null,
                        false,
                        false,
                        false,
                        List.copyOf(warnings)
                ),
                MutableNutrition.zero(),
                null
        );
    }

    private NutritionCalculation calculateNutrition(
            ProductEntity product,
            BigDecimal quantity,
            List<String> warnings
    ) {
        var nutrition = product.getNutrition();
        if (nutrition == null) {
            warnings.add("No hay información nutricional para " + product.getName() + ".");
            return new NutritionCalculation(null, false);
        }
        if (product.getMeasurementType()
                == com.sean.supermarketmealplanner.catalog.domain.MeasurementType.UNIT) {
            if (nutrition.getCaloriesPerUnit() == null
                    || nutrition.getProteinPerUnit() == null
                    || nutrition.getCarbohydratesPerUnit() == null
                    || nutrition.getFatPerUnit() == null) {
                warnings.add(
                        "No hay valores nutricionales por unidad para " + product.getName() + "."
                );
                return new NutritionCalculation(null, false);
            }
            return new NutritionCalculation(
                    MutableNutrition.fromPerUnit(nutrition).multipliedBy(quantity),
                    true
            );
        }
        var factor = quantity.divide(ONE_HUNDRED, MATH_CONTEXT);
        return new NutritionCalculation(
                MutableNutrition.fromPerHundred(nutrition).multipliedBy(factor),
                true
        );
    }

    private CostCalculation calculateCost(
            ProductEntity product,
            BigDecimal quantity,
            List<String> warnings
    ) {
        if (!product.isCostDataComplete()
                || product.getCurrentPrice() == null
                || product.getPackageQuantity() == null) {
            warnings.add("El coste no se puede calcular para " + product.getName() + ".");
            return new CostCalculation(null, false);
        }
        var packageBaseQuantity = packageBaseQuantity(product);
        if (packageBaseQuantity == null || packageBaseQuantity.signum() <= 0) {
            warnings.add("Falta un formato de paquete compatible para " + product.getName() + ".");
            return new CostCalculation(null, false);
        }
        return new CostCalculation(
                product.getCurrentPrice()
                        .multiply(quantity, MATH_CONTEXT)
                        .divide(packageBaseQuantity, MATH_CONTEXT),
                true
        );
    }

    private BigDecimal packageBaseQuantity(ProductEntity product) {
        PackageUnit unit = product.getPackageUnit();
        return switch (unit) {
            case G, ML, UNIT -> product.getPackageQuantity();
            case KG, L -> product.getPackageQuantity().multiply(ONE_THOUSAND, MATH_CONTEXT);
        };
    }

    private static BigDecimal nutrient(BigDecimal value) {
        return value == null ? null : value.setScale(1, ROUNDING_MODE);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, ROUNDING_MODE);
    }

    public record Evaluation(
            MealTemplateResponse response,
            List<Set<String>> mandatoryProductTagSets,
            Set<String> mandatoryAllergens,
            Set<String> optionalAllergens
    ) {
    }

    private record IngredientCalculation(
            MealTemplateIngredientResponse response,
            MutableNutrition rawNutrition,
            BigDecimal rawCost
    ) {
    }

    private record NutritionCalculation(MutableNutrition values, boolean complete) {
    }

    private record CostCalculation(BigDecimal value, boolean complete) {
    }

    private static final class MutableNutrition {
        private BigDecimal calories;
        private BigDecimal protein;
        private BigDecimal carbohydrates;
        private BigDecimal fat;
        private BigDecimal fiber;
        private BigDecimal sugar;
        private BigDecimal salt;

        private MutableNutrition(
                BigDecimal calories,
                BigDecimal protein,
                BigDecimal carbohydrates,
                BigDecimal fat,
                BigDecimal fiber,
                BigDecimal sugar,
                BigDecimal salt
        ) {
            this.calories = orZero(calories);
            this.protein = orZero(protein);
            this.carbohydrates = orZero(carbohydrates);
            this.fat = orZero(fat);
            this.fiber = orZero(fiber);
            this.sugar = orZero(sugar);
            this.salt = orZero(salt);
        }

        static MutableNutrition zero() {
            return new MutableNutrition(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }

        static MutableNutrition fromPerHundred(NutritionEntity nutrition) {
            return new MutableNutrition(
                    nutrition.getCaloriesPer100g(),
                    nutrition.getProteinPer100g(),
                    nutrition.getCarbohydratesPer100g(),
                    nutrition.getFatPer100g(),
                    nutrition.getFiberPer100g(),
                    nutrition.getSugarPer100g(),
                    nutrition.getSaltPer100g()
            );
        }

        static MutableNutrition fromPerUnit(NutritionEntity nutrition) {
            return new MutableNutrition(
                    nutrition.getCaloriesPerUnit(),
                    nutrition.getProteinPerUnit(),
                    nutrition.getCarbohydratesPerUnit(),
                    nutrition.getFatPerUnit(),
                    nutrition.getFiberPerUnit(),
                    nutrition.getSugarPerUnit(),
                    nutrition.getSaltPerUnit()
            );
        }

        MutableNutrition multipliedBy(BigDecimal factor) {
            return new MutableNutrition(
                    calories.multiply(factor, MATH_CONTEXT),
                    protein.multiply(factor, MATH_CONTEXT),
                    carbohydrates.multiply(factor, MATH_CONTEXT),
                    fat.multiply(factor, MATH_CONTEXT),
                    fiber.multiply(factor, MATH_CONTEXT),
                    sugar.multiply(factor, MATH_CONTEXT),
                    salt.multiply(factor, MATH_CONTEXT)
            );
        }

        MutableNutrition dividedBy(BigDecimal divisor) {
            return new MutableNutrition(
                    calories.divide(divisor, MATH_CONTEXT),
                    protein.divide(divisor, MATH_CONTEXT),
                    carbohydrates.divide(divisor, MATH_CONTEXT),
                    fat.divide(divisor, MATH_CONTEXT),
                    fiber.divide(divisor, MATH_CONTEXT),
                    sugar.divide(divisor, MATH_CONTEXT),
                    salt.divide(divisor, MATH_CONTEXT)
            );
        }

        void add(MutableNutrition other) {
            calories = calories.add(other.calories, MATH_CONTEXT);
            protein = protein.add(other.protein, MATH_CONTEXT);
            carbohydrates = carbohydrates.add(other.carbohydrates, MATH_CONTEXT);
            fat = fat.add(other.fat, MATH_CONTEXT);
            fiber = fiber.add(other.fiber, MATH_CONTEXT);
            sugar = sugar.add(other.sugar, MATH_CONTEXT);
            salt = salt.add(other.salt, MATH_CONTEXT);
        }

        NutritionBreakdown rounded() {
            return new NutritionBreakdown(
                    nutrient(calories),
                    nutrient(protein),
                    nutrient(carbohydrates),
                    nutrient(fat),
                    nutrient(fiber),
                    nutrient(sugar),
                    nutrient(salt)
            );
        }

        private static BigDecimal orZero(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }
}
