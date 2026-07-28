package com.sean.supermarketmealplanner.mealplan.infrastructure.persistence;

import com.sean.supermarketmealplanner.mealplan.application.GeneratedMealPlanResult;
import com.sean.supermarketmealplanner.mealplan.domain.WarningSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "meal_plan_warnings")
public class MealPlanWarningEntity {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlanEntity mealPlan;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_day_id")
    private MealPlanDayEntity mealPlanDay;
    @Column(name = "warning_code", nullable = false, length = 80)
    private String warningCode;
    @Column(nullable = false, length = 1000)
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WarningSeverity severity;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected MealPlanWarningEntity() {
    }

    MealPlanWarningEntity(
            MealPlanEntity plan,
            MealPlanDayEntity day,
            GeneratedMealPlanResult.PlanWarning warning
    ) {
        this.id = UUID.randomUUID();
        this.mealPlan = plan;
        this.mealPlanDay = day;
        this.warningCode = warning.code();
        this.message = warning.message();
        this.severity = warning.severity();
        this.createdAt = OffsetDateTime.now();
    }
}
