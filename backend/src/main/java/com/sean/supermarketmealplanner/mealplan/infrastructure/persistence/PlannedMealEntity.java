package com.sean.supermarketmealplanner.mealplan.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.mealplan.domain.MealSelectionSource;
import com.sean.supermarketmealplanner.mealtemplate.domain.MealType;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "planned_meals")
public class PlannedMealEntity {

    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_day_id", nullable = false)
    private MealPlanDayEntity mealPlanDay;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_template_id", nullable = false)
    private MealTemplateEntity mealTemplate;
    @Column(name = "template_name", nullable = false, length = 180)
    private String templateName;
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;
    @Column(nullable = false)
    private int position;
    @Column(nullable = false)
    private int servings;
    @Column(name = "ingredients_json", nullable = false, columnDefinition = "TEXT")
    private String ingredientsJson;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal calories;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal protein;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal carbohydrates;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal fat;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal fiber;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal sugar;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal salt;
    @Column(name = "consumed_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal consumedCost;
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;
    @Column(name = "calculation_complete", nullable = false)
    private boolean calculationComplete;
    @Column(name = "warnings_json", nullable = false, columnDefinition = "TEXT")
    private String warningsJson;
    @Column(nullable = false)
    private boolean locked;
    @Enumerated(EnumType.STRING)
    @Column(name = "selection_source", nullable = false, length = 30)
    private MealSelectionSource selectionSource;
    @Column(name = "edit_version", nullable = false)
    private long editVersion;
    @Column(name = "modified_at")
    private OffsetDateTime modifiedAt;
    @Column(name = "original_meal_template_id")
    private UUID originalMealTemplateId;
    @Column(name = "partial_generation_seed")
    private Long partialGenerationSeed;

    protected PlannedMealEntity() {
    }

    PlannedMealEntity(
            MealPlanDayEntity day,
            MealTemplateEntity template,
            GeneratedMealPlanResult.PlannedMealResult meal
    ) {
        if (template == null) {
            throw new IllegalArgumentException("Meal template snapshot reference is missing");
        }
        this.id = meal.plannedMealId() == null ? UUID.randomUUID() : meal.plannedMealId();
        this.mealPlanDay = day;
        this.mealTemplate = template;
        this.templateName = meal.templateName();
        this.mealType = MealType.valueOf(meal.mealType());
        this.position = meal.position();
        this.servings = meal.servings();
        this.ingredientsJson = json(meal.ingredients());
        this.calories = meal.nutrition().calories();
        this.protein = meal.nutrition().protein();
        this.carbohydrates = meal.nutrition().carbohydrates();
        this.fat = meal.nutrition().fat();
        this.fiber = meal.nutrition().fiber();
        this.sugar = meal.nutrition().sugar();
        this.salt = meal.nutrition().salt();
        this.consumedCost = meal.consumedCost();
        this.score = meal.score();
        this.calculationComplete = meal.calculationComplete();
        this.warningsJson = json(meal.warnings());
        this.locked = meal.locked();
        this.selectionSource = meal.selectionSource() == null
                ? MealSelectionSource.GENERATED
                : meal.selectionSource();
        this.editVersion = meal.editVersion();
        this.modifiedAt = meal.modifiedAt();
        this.originalMealTemplateId = meal.originalMealTemplateId();
        this.partialGenerationSeed = meal.partialGenerationSeed();
    }

    private String json(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize planned meal snapshot", exception);
        }
    }

    public void setLocked(boolean value, long version, OffsetDateTime now) {
        this.locked = value;
        this.editVersion = version;
        this.modifiedAt = now;
    }

    public void replace(
            MealTemplateEntity template,
            GeneratedMealPlanResult.PlannedMealResult meal,
            MealSelectionSource source,
            long version,
            Long seed,
            OffsetDateTime now
    ) {
        if (originalMealTemplateId == null) {
            originalMealTemplateId = mealTemplate.getId();
        }
        this.mealTemplate = template;
        this.templateName = meal.templateName();
        this.mealType = MealType.valueOf(meal.mealType());
        this.servings = meal.servings();
        this.ingredientsJson = json(meal.ingredients());
        this.calories = meal.nutrition().calories();
        this.protein = meal.nutrition().protein();
        this.carbohydrates = meal.nutrition().carbohydrates();
        this.fat = meal.nutrition().fat();
        this.fiber = meal.nutrition().fiber();
        this.sugar = meal.nutrition().sugar();
        this.salt = meal.nutrition().salt();
        this.consumedCost = meal.consumedCost();
        this.score = meal.score();
        this.calculationComplete = meal.calculationComplete();
        this.warningsJson = json(meal.warnings());
        this.selectionSource = source;
        this.editVersion = version;
        this.modifiedAt = now;
        this.partialGenerationSeed = seed;
    }

    public UUID getId() { return id; }
    public MealPlanDayEntity getMealPlanDay() { return mealPlanDay; }
    public MealTemplateEntity getMealTemplate() { return mealTemplate; }
    public int getPosition() { return position; }
    public boolean isLocked() { return locked; }
    public MealSelectionSource getSelectionSource() { return selectionSource; }
    public long getEditVersion() { return editVersion; }
    public OffsetDateTime getModifiedAt() { return modifiedAt; }
    public UUID getOriginalMealTemplateId() { return originalMealTemplateId; }
    public Long getPartialGenerationSeed() { return partialGenerationSeed; }
}
