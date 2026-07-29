package com.sean.supermarketmealplanner.mealplan.infrastructure.persistence;

import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence.MealTemplateEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "meal_plan_days")
public class MealPlanDayEntity {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlanEntity mealPlan;
    @Column(name = "day_index", nullable = false)
    private int dayIndex;
    @Column(name = "plan_date", nullable = false)
    private LocalDate date;
    @Column(name = "total_calories", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalCalories;
    @Column(name = "total_protein", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalProtein;
    @Column(name = "total_carbohydrates", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalCarbohydrates;
    @Column(name = "total_fat", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalFat;
    @Column(name = "total_fiber", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalFiber;
    @Column(name = "total_sugar", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalSugar;
    @Column(name = "total_salt", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalSalt;
    @Column(name = "total_consumed_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalConsumedCost;
    @Column(name = "calorie_deviation", nullable = false, precision = 12, scale = 3)
    private BigDecimal calorieDeviation;
    @Column(name = "protein_deviation", nullable = false, precision = 12, scale = 3)
    private BigDecimal proteinDeviation;
    @Column(name = "daily_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal dailyScore;

    @OneToMany(mappedBy = "mealPlanDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PlannedMealEntity> meals = new ArrayList<>();

    protected MealPlanDayEntity() {
    }

    MealPlanDayEntity(
            MealPlanEntity plan,
            GeneratedMealPlanResult.DayResult day,
            Map<UUID, MealTemplateEntity> templates
    ) {
        this.id = day.dayId() == null ? UUID.randomUUID() : day.dayId();
        this.mealPlan = plan;
        this.dayIndex = day.dayIndex();
        this.date = day.date();
        this.totalCalories = day.totalNutrition().calories();
        this.totalProtein = day.totalNutrition().protein();
        this.totalCarbohydrates = day.totalNutrition().carbohydrates();
        this.totalFat = day.totalNutrition().fat();
        this.totalFiber = day.totalNutrition().fiber();
        this.totalSugar = day.totalNutrition().sugar();
        this.totalSalt = day.totalNutrition().salt();
        this.totalConsumedCost = day.totalConsumedCost();
        this.calorieDeviation = day.calorieDeviation();
        this.proteinDeviation = day.proteinDeviation();
        this.dailyScore = day.dailyScore();
        day.meals().forEach(meal -> this.meals.add(new PlannedMealEntity(
                this,
                templates.get(meal.templateId()),
                meal
        )));
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public UUID getId() { return id; }
    public LocalDate getDate() { return date; }
    public List<PlannedMealEntity> getMeals() { return List.copyOf(meals); }

    public void updateTotals(GeneratedMealPlanResult.DayResult day) {
        this.totalCalories = day.totalNutrition().calories();
        this.totalProtein = day.totalNutrition().protein();
        this.totalCarbohydrates = day.totalNutrition().carbohydrates();
        this.totalFat = day.totalNutrition().fat();
        this.totalFiber = day.totalNutrition().fiber();
        this.totalSugar = day.totalNutrition().sugar();
        this.totalSalt = day.totalNutrition().salt();
        this.totalConsumedCost = day.totalConsumedCost();
        this.calorieDeviation = day.calorieDeviation();
        this.proteinDeviation = day.proteinDeviation();
        this.dailyScore = day.dailyScore();
    }
}
