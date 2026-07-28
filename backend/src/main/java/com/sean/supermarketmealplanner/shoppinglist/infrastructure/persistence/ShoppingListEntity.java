package com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.mealplan.infrastructure.persistence.MealPlanEntity;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListCalculation;
import com.sean.supermarketmealplanner.shoppinglist.domain.ShoppingListStatus;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Table(name = "shopping_lists")
public class ShoppingListEntity {

    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlanEntity mealPlan;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supermarket_id", nullable = false)
    private SupermarketEntity supermarket;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShoppingListStatus status;
    @Column(name = "total_packages", nullable = false)
    private int totalPackages;
    @Column(name = "total_consumed_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalConsumedCost;
    @Column(name = "total_purchase_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPurchaseCost;
    @Column(name = "total_waste_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalWasteCost;
    @Column(name = "overall_waste_percentage", nullable = false, precision = 6, scale = 1)
    private BigDecimal overallWastePercentage;
    @Column(name = "quantity_summary_json", nullable = false, columnDefinition = "TEXT")
    private String quantitySummaryJson;
    @Column(name = "weekly_budget", precision = 14, scale = 2)
    private BigDecimal weeklyBudget;
    @Column(name = "purchase_budget_difference", precision = 14, scale = 2)
    private BigDecimal purchaseBudgetDifference;
    @Column(name = "purchase_budget_exceeded", nullable = false)
    private boolean purchaseBudgetExceeded;
    @Column(name = "budget_calculation_complete", nullable = false)
    private boolean budgetCalculationComplete;
    @Column(name = "calculation_complete", nullable = false)
    private boolean calculationComplete;
    @Column(name = "generation_duration_milliseconds", nullable = false)
    private long generationDurationMilliseconds;
    @Column(name = "demo_data", nullable = false)
    private boolean demoData;
    @Column(nullable = false)
    private boolean archived;
    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "shoppingList", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ShoppingListItemEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "shoppingList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShoppingListWarningEntity> warnings = new ArrayList<>();

    protected ShoppingListEntity() {
    }

    public ShoppingListEntity(
            MealPlanEntity mealPlan,
            ShoppingListCalculation calculation,
            long durationMilliseconds,
            OffsetDateTime now
    ) {
        this.id = UUID.randomUUID();
        this.mealPlan = mealPlan;
        this.supermarket = mealPlan.getSupermarket();
        this.status = ShoppingListStatus.GENERATED;
        this.totalPackages = calculation.totalPackages();
        this.totalConsumedCost = calculation.totalConsumedCost();
        this.totalPurchaseCost = calculation.totalPurchaseCost();
        this.totalWasteCost = calculation.totalWasteCost();
        this.overallWastePercentage = calculation.overallWastePercentage();
        this.quantitySummaryJson = json(calculation.quantitySummary());
        this.weeklyBudget = calculation.weeklyBudget();
        this.purchaseBudgetDifference = calculation.purchaseBudgetDifference();
        this.purchaseBudgetExceeded = calculation.purchaseBudgetExceeded();
        this.budgetCalculationComplete = calculation.budgetCalculationComplete();
        this.calculationComplete = calculation.calculationComplete();
        this.generationDurationMilliseconds = durationMilliseconds;
        this.demoData = true;
        this.archived = false;
        this.generatedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
        calculation.items().forEach(item -> items.add(new ShoppingListItemEntity(this, item, now)));
        var byId = items.stream().collect(Collectors.toMap(
                ShoppingListItemEntity::getId,
                Function.identity()
        ));
        calculation.warnings().forEach(warning -> warnings.add(new ShoppingListWarningEntity(
                this,
                warning.itemId() == null ? null : byId.get(warning.itemId()),
                warning,
                now
        )));
    }

    public void changeStatus(ShoppingListStatus requested, OffsetDateTime now) {
        this.status = requested;
        this.archived = requested == ShoppingListStatus.ARCHIVED;
        this.updatedAt = now;
    }

    private String json(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize quantity summary", exception);
        }
    }

    public UUID getId() { return id; }
    public MealPlanEntity getMealPlan() { return mealPlan; }
    public SupermarketEntity getSupermarket() { return supermarket; }
    public ShoppingListStatus getStatus() { return status; }
    public int getTotalPackages() { return totalPackages; }
    public BigDecimal getTotalConsumedCost() { return totalConsumedCost; }
    public BigDecimal getTotalPurchaseCost() { return totalPurchaseCost; }
    public BigDecimal getTotalWasteCost() { return totalWasteCost; }
    public BigDecimal getOverallWastePercentage() { return overallWastePercentage; }
    public String getQuantitySummaryJson() { return quantitySummaryJson; }
    public BigDecimal getWeeklyBudget() { return weeklyBudget; }
    public BigDecimal getPurchaseBudgetDifference() { return purchaseBudgetDifference; }
    public boolean isPurchaseBudgetExceeded() { return purchaseBudgetExceeded; }
    public boolean isBudgetCalculationComplete() { return budgetCalculationComplete; }
    public boolean isCalculationComplete() { return calculationComplete; }
    public long getGenerationDurationMilliseconds() { return generationDurationMilliseconds; }
    public boolean isDemoData() { return demoData; }
    public boolean isArchived() { return archived; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<ShoppingListItemEntity> getItems() { return List.copyOf(items); }
    public List<ShoppingListWarningEntity> getWarnings() { return List.copyOf(warnings); }
}
