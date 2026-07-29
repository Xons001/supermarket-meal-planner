package com.sean.supermarketmealplanner.mealplan.infrastructure.persistence;

import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserAccountEntity;
import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealplan.domain.MealPlanStatus;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "meal_plans")
public class MealPlanEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supermarket_id", nullable = false)
    private SupermarketEntity supermarket;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccountEntity owner;

    @Column(nullable = false, length = 180)
    private String name;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "number_of_days", nullable = false)
    private int numberOfDays;
    @Column(name = "meals_per_day", nullable = false)
    private int mealsPerDay;
    @Column(nullable = false)
    private int servings;
    @Column(name = "daily_calories_target", nullable = false, precision = 12, scale = 3)
    private BigDecimal dailyCaloriesTarget;
    @Column(name = "daily_protein_target", nullable = false, precision = 12, scale = 3)
    private BigDecimal dailyProteinTarget;
    @Column(name = "weekly_budget", precision = 12, scale = 2)
    private BigDecimal weeklyBudget;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MealPlanStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_strategy", nullable = false, length = 30)
    private GenerationStrategy generationStrategy;
    @Column(name = "deterministic_seed", nullable = false)
    private long deterministicSeed;
    @Column(name = "criteria_json", nullable = false, columnDefinition = "TEXT")
    private String criteriaJson;
    @Column(name = "result_json", nullable = false, columnDefinition = "TEXT")
    private String resultJson;
    @Column(name = "generation_token", nullable = false, length = 64)
    private String generationToken;
    @Column(name = "total_calories", nullable = false, precision = 14, scale = 3)
    private BigDecimal totalCalories;
    @Column(name = "total_protein", nullable = false, precision = 14, scale = 3)
    private BigDecimal totalProtein;
    @Column(name = "total_carbohydrates", nullable = false, precision = 14, scale = 3)
    private BigDecimal totalCarbohydrates;
    @Column(name = "total_fat", nullable = false, precision = 14, scale = 3)
    private BigDecimal totalFat;
    @Column(name = "total_fiber", nullable = false, precision = 14, scale = 3)
    private BigDecimal totalFiber;
    @Column(name = "total_sugar", nullable = false, precision = 14, scale = 3)
    private BigDecimal totalSugar;
    @Column(name = "total_salt", nullable = false, precision = 14, scale = 3)
    private BigDecimal totalSalt;
    @Column(name = "total_consumed_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalConsumedCost;
    @Column(name = "overall_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal overallScore;
    @Column(name = "calorie_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal calorieScore;
    @Column(name = "protein_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal proteinScore;
    @Column(name = "budget_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal budgetScore;
    @Column(name = "variety_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal varietyScore;
    @Column(name = "repetition_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal repetitionScore;
    @Column(name = "completeness_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal completenessScore;
    @Column(name = "preparation_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal preparationScore;
    @Column(name = "unique_templates", nullable = false)
    private int uniqueTemplates;
    @Column(name = "repeated_templates", nullable = false)
    private int repeatedTemplates;
    @Column(name = "maximum_observed_repetition", nullable = false)
    private int maximumObservedRepetition;
    @Column(name = "calculation_complete", nullable = false)
    private boolean calculationComplete;
    @Column(name = "candidates_evaluated", nullable = false)
    private int candidatesEvaluated;
    @Column(name = "complete_plans_evaluated", nullable = false)
    private int completePlansEvaluated;
    @Column(name = "duration_milliseconds", nullable = false)
    private long durationMilliseconds;
    @Column(name = "algorithm_version", nullable = false, length = 40)
    private String algorithmVersion;
    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;
    @Column(nullable = false)
    private boolean archived;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Column(name = "edit_version", nullable = false)
    private long editVersion;
    @Column(name = "content_version", nullable = false)
    private long contentVersion;
    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayIndex ASC")
    private List<MealPlanDayEntity> days = new ArrayList<>();

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealPlanWarningEntity> warnings = new ArrayList<>();

    protected MealPlanEntity() {
    }

    public MealPlanEntity(
            GeneratedMealPlanResult result,
            SupermarketEntity supermarket,
            UserAccountEntity owner,
            String criteriaJson,
            String resultJson,
            Map<UUID, com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateEntity> templates
    ) {
        this.id = result.mealPlanId();
        this.supermarket = supermarket;
        this.owner = owner;
        this.name = result.name();
        this.startDate = result.startDate();
        this.numberOfDays = result.numberOfDays();
        this.mealsPerDay = result.mealsPerDay();
        this.servings = result.servings();
        this.dailyCaloriesTarget = result.criteria().dailyCaloriesTarget();
        this.dailyProteinTarget = result.criteria().dailyProteinTarget();
        this.weeklyBudget = result.weeklyBudget();
        this.status = result.status();
        this.generationStrategy = result.strategy();
        this.deterministicSeed = result.seed();
        this.criteriaJson = criteriaJson;
        this.resultJson = resultJson;
        this.generationToken = result.generationToken();
        this.totalCalories = result.weeklyNutrition().calories();
        this.totalProtein = result.weeklyNutrition().protein();
        this.totalCarbohydrates = result.weeklyNutrition().carbohydrates();
        this.totalFat = result.weeklyNutrition().fat();
        this.totalFiber = result.weeklyNutrition().fiber();
        this.totalSugar = result.weeklyNutrition().sugar();
        this.totalSalt = result.weeklyNutrition().salt();
        this.totalConsumedCost = result.totalConsumedCost();
        this.overallScore = result.overallScore();
        this.calorieScore = result.scoreBreakdown().calorieScore();
        this.proteinScore = result.scoreBreakdown().proteinScore();
        this.budgetScore = result.scoreBreakdown().budgetScore();
        this.varietyScore = result.scoreBreakdown().varietyScore();
        this.repetitionScore = result.scoreBreakdown().repetitionScore();
        this.completenessScore = result.scoreBreakdown().completenessScore();
        this.preparationScore = result.scoreBreakdown().preparationScore();
        this.uniqueTemplates = result.varietyMetrics().uniqueTemplates();
        this.repeatedTemplates = result.varietyMetrics().repeatedTemplates();
        this.maximumObservedRepetition = result.varietyMetrics().maximumObservedRepetition();
        this.calculationComplete = result.calculationComplete();
        this.candidatesEvaluated = result.generationMetadata().candidatesEvaluated();
        this.completePlansEvaluated = result.generationMetadata().completePlansEvaluated();
        this.durationMilliseconds = result.generationMetadata().durationMilliseconds();
        this.algorithmVersion = result.generationMetadata().algorithmVersion();
        this.generatedAt = result.generationMetadata().generatedAt();
        this.archived = false;
        this.createdAt = result.createdAt();
        this.updatedAt = result.updatedAt();
        this.editVersion = result.editVersion();
        this.contentVersion = result.contentVersion();
        result.days().forEach(day -> this.days.add(new MealPlanDayEntity(this, day, templates)));
        var daysByIndex = this.days.stream()
                .collect(java.util.stream.Collectors.toMap(
                        MealPlanDayEntity::getDayIndex,
                        java.util.function.Function.identity()
                ));
        result.warnings().forEach(warning -> this.warnings.add(new MealPlanWarningEntity(
                this,
                warning.dayIndex() == null ? null : daysByIndex.get(warning.dayIndex()),
                warning
        )));
    }

    public void changeStatus(
            MealPlanStatus requestedStatus,
            String updatedResultJson,
            OffsetDateTime changedAt
    ) {
        if (requestedStatus == MealPlanStatus.DRAFT) {
            throw new IllegalArgumentException("A persisted plan cannot return to DRAFT");
        }
        this.status = requestedStatus;
        this.archived = requestedStatus == MealPlanStatus.ARCHIVED;
        this.updatedAt = changedAt;
        this.resultJson = updatedResultJson;
    }

    public VersionChange nextVersion(boolean contentChanged) {
        var before = new VersionChange(editVersion, contentVersion);
        editVersion++;
        if (contentChanged) {
            contentVersion++;
        }
        return before;
    }

    public void updateEditedSnapshot(
            GeneratedMealPlanResult result,
            String updatedResultJson,
            OffsetDateTime changedAt
    ) {
        this.resultJson = updatedResultJson;
        this.totalCalories = result.weeklyNutrition().calories();
        this.totalProtein = result.weeklyNutrition().protein();
        this.totalCarbohydrates = result.weeklyNutrition().carbohydrates();
        this.totalFat = result.weeklyNutrition().fat();
        this.totalFiber = result.weeklyNutrition().fiber();
        this.totalSugar = result.weeklyNutrition().sugar();
        this.totalSalt = result.weeklyNutrition().salt();
        this.totalConsumedCost = result.totalConsumedCost();
        this.overallScore = result.overallScore();
        this.calorieScore = result.scoreBreakdown().calorieScore();
        this.proteinScore = result.scoreBreakdown().proteinScore();
        this.budgetScore = result.scoreBreakdown().budgetScore();
        this.varietyScore = result.scoreBreakdown().varietyScore();
        this.repetitionScore = result.scoreBreakdown().repetitionScore();
        this.completenessScore = result.scoreBreakdown().completenessScore();
        this.preparationScore = result.scoreBreakdown().preparationScore();
        this.uniqueTemplates = result.varietyMetrics().uniqueTemplates();
        this.repeatedTemplates = result.varietyMetrics().repeatedTemplates();
        this.maximumObservedRepetition = result.varietyMetrics().maximumObservedRepetition();
        this.calculationComplete = result.calculationComplete();
        this.updatedAt = changedAt;
    }

    public UUID getId() { return id; }
    public SupermarketEntity getSupermarket() { return supermarket; }
    public UserAccountEntity getOwner() { return owner; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public int getNumberOfDays() { return numberOfDays; }
    public int getMealsPerDay() { return mealsPerDay; }
    public int getServings() { return servings; }
    public BigDecimal getDailyCaloriesTarget() { return dailyCaloriesTarget; }
    public BigDecimal getDailyProteinTarget() { return dailyProteinTarget; }
    public BigDecimal getWeeklyBudget() { return weeklyBudget; }
    public MealPlanStatus getStatus() { return status; }
    public long getDeterministicSeed() { return deterministicSeed; }
    public String getCriteriaJson() { return criteriaJson; }
    public String getResultJson() { return resultJson; }
    public String getGenerationToken() { return generationToken; }
    public BigDecimal getTotalConsumedCost() { return totalConsumedCost; }
    public BigDecimal getOverallScore() { return overallScore; }
    public boolean isCalculationComplete() { return calculationComplete; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<MealPlanWarningEntity> getWarnings() { return List.copyOf(warnings); }
    public List<MealPlanDayEntity> getDays() { return List.copyOf(days); }
    public long getEditVersion() { return editVersion; }
    public long getContentVersion() { return contentVersion; }
    public long getRowVersion() { return rowVersion; }

    public record VersionChange(long editVersion, long contentVersion) {
    }
}
