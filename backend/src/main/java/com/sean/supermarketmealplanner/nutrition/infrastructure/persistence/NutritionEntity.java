package com.sean.supermarketmealplanner.nutrition.infrastructure.persistence;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "nutrition")
public class NutritionEntity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private ProductEntity product;

    @Column(name = "calories_per_100g", nullable = false, precision = 10, scale = 2)
    private BigDecimal caloriesPer100g;

    @Column(name = "protein_per_100g", nullable = false, precision = 10, scale = 2)
    private BigDecimal proteinPer100g;

    @Column(name = "carbohydrates_per_100g", nullable = false, precision = 10, scale = 2)
    private BigDecimal carbohydratesPer100g;

    @Column(name = "fat_per_100g", nullable = false, precision = 10, scale = 2)
    private BigDecimal fatPer100g;

    @Column(name = "fiber_per_100g", nullable = false, precision = 10, scale = 2)
    private BigDecimal fiberPer100g;

    @Column(name = "sugar_per_100g", nullable = false, precision = 10, scale = 2)
    private BigDecimal sugarPer100g;

    @Column(name = "salt_per_100g", nullable = false, precision = 10, scale = 2)
    private BigDecimal saltPer100g;

    @Column(name = "data_source", nullable = false, length = 80)
    private String dataSource;

    @Column(name = "verification_status", nullable = false, length = 40)
    private String verificationStatus;

    @Column(name = "confidence_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal confidenceScore;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected NutritionEntity() {
    }

    public NutritionEntity(ProductEntity product) {
        this.id = UUID.randomUUID();
        this.product = product;
    }

    public void update(
            BigDecimal caloriesPer100g,
            BigDecimal proteinPer100g,
            BigDecimal carbohydratesPer100g,
            BigDecimal fatPer100g,
            BigDecimal fiberPer100g,
            BigDecimal sugarPer100g,
            BigDecimal saltPer100g,
            String dataSource,
            String verificationStatus,
            BigDecimal confidenceScore,
            OffsetDateTime updatedAt
    ) {
        this.caloriesPer100g = caloriesPer100g;
        this.proteinPer100g = proteinPer100g;
        this.carbohydratesPer100g = carbohydratesPer100g;
        this.fatPer100g = fatPer100g;
        this.fiberPer100g = fiberPer100g;
        this.sugarPer100g = sugarPer100g;
        this.saltPer100g = saltPer100g;
        this.dataSource = dataSource;
        this.verificationStatus = verificationStatus;
        this.confidenceScore = confidenceScore;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public BigDecimal getCaloriesPer100g() {
        return caloriesPer100g;
    }

    public BigDecimal getProteinPer100g() {
        return proteinPer100g;
    }

    public BigDecimal getCarbohydratesPer100g() {
        return carbohydratesPer100g;
    }

    public BigDecimal getFatPer100g() {
        return fatPer100g;
    }

    public BigDecimal getFiberPer100g() {
        return fiberPer100g;
    }

    public BigDecimal getSugarPer100g() {
        return sugarPer100g;
    }

    public BigDecimal getSaltPer100g() {
        return saltPer100g;
    }

    public String getDataSource() {
        return dataSource;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
