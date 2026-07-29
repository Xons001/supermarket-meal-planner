package com.sean.supermarketmealplanner.identity.infrastructure.persistence;

import com.sean.supermarketmealplanner.mealplan.domain.GenerationStrategy;
import com.sean.supermarketmealplanner.mealplan.domain.OptimizationPreset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.sean.supermarketmealplanner.identity.domain.ThemePreference;

@Entity
@Table(name = "user_preferences")
public class UserPreferencesEntity {
    @Id
    private UUID userId;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccountEntity user;
    @Column(name = "daily_calories_target", nullable = false, precision = 12, scale = 3)
    private BigDecimal dailyCaloriesTarget;
    @Column(name = "daily_protein_target", nullable = false, precision = 12, scale = 3)
    private BigDecimal dailyProteinTarget;
    @Column(name = "weekly_budget", precision = 12, scale = 2)
    private BigDecimal weeklyBudget;
    @Column(name = "number_of_days", nullable = false)
    private int numberOfDays;
    @Column(name = "meals_per_day", nullable = false)
    private int mealsPerDay;
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_strategy", nullable = false, length = 30)
    private GenerationStrategy strategy;
    @Enumerated(EnumType.STRING)
    @Column(name = "optimization_preset", length = 30)
    private OptimizationPreset preset;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dietary_restrictions", nullable = false, columnDefinition = "jsonb")
    private List<String> dietaryRestrictions;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> allergens;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ThemePreference theme;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserPreferencesEntity() {}

    public UserPreferencesEntity(UserAccountEntity user, OffsetDateTime now) {
        this.user = user;
        this.dailyCaloriesTarget = new BigDecimal("2000");
        this.dailyProteinTarget = new BigDecimal("100");
        this.weeklyBudget = new BigDecimal("70");
        this.numberOfDays = 7;
        this.mealsPerDay = 4;
        this.strategy = GenerationStrategy.PURCHASE_AWARE_SCORING;
        this.preset = OptimizationPreset.BALANCED;
        this.dietaryRestrictions = new ArrayList<>();
        this.allergens = new ArrayList<>();
        this.theme = ThemePreference.SYSTEM;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(BigDecimal calories, BigDecimal protein, BigDecimal budget, int days, int meals,
                       GenerationStrategy strategy, OptimizationPreset preset, List<String> dietary,
                       List<String> allergens, OffsetDateTime now) {
        update(calories, protein, budget, days, meals, strategy, preset, dietary, allergens, this.theme, now);
    }

    public void update(BigDecimal calories, BigDecimal protein, BigDecimal budget, int days, int meals,
                       GenerationStrategy strategy, OptimizationPreset preset, List<String> dietary,
                       List<String> allergens, ThemePreference theme, OffsetDateTime now) {
        this.dailyCaloriesTarget = calories;
        this.dailyProteinTarget = protein;
        this.weeklyBudget = budget;
        this.numberOfDays = days;
        this.mealsPerDay = meals;
        this.strategy = strategy;
        this.preset = strategy == GenerationStrategy.SCORING ? null : preset;
        this.dietaryRestrictions = List.copyOf(dietary);
        this.allergens = List.copyOf(allergens);
        this.theme = theme == null ? ThemePreference.SYSTEM : theme;
        this.updatedAt = now;
    }

    public UUID getUserId() { return userId; }
    public BigDecimal getDailyCaloriesTarget() { return dailyCaloriesTarget; }
    public BigDecimal getDailyProteinTarget() { return dailyProteinTarget; }
    public BigDecimal getWeeklyBudget() { return weeklyBudget; }
    public int getNumberOfDays() { return numberOfDays; }
    public int getMealsPerDay() { return mealsPerDay; }
    public GenerationStrategy getStrategy() { return strategy; }
    public OptimizationPreset getPreset() { return preset; }
    public List<String> getDietaryRestrictions() { return List.copyOf(dietaryRestrictions); }
    public List<String> getAllergens() { return List.copyOf(allergens); }
    public ThemePreference getTheme() { return theme; }
}
