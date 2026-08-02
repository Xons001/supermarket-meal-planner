package com.sean.supermarketmealplanner.nutrition.infrastructure.persistence;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

    @Column(name = "calories_per_100g", precision = 10, scale = 2)
    private BigDecimal caloriesPer100g;

    @Column(name = "protein_per_100g", precision = 10, scale = 2)
    private BigDecimal proteinPer100g;

    @Column(name = "carbohydrates_per_100g", precision = 10, scale = 2)
    private BigDecimal carbohydratesPer100g;

    @Column(name = "fat_per_100g", precision = 10, scale = 2)
    private BigDecimal fatPer100g;

    @Column(name = "fiber_per_100g", precision = 10, scale = 2)
    private BigDecimal fiberPer100g;

    @Column(name = "sugar_per_100g", precision = 10, scale = 2)
    private BigDecimal sugarPer100g;

    @Column(name = "salt_per_100g", precision = 10, scale = 2)
    private BigDecimal saltPer100g;

    @Column(name = "saturated_fat_per_100g", precision = 10, scale = 2)
    private BigDecimal saturatedFatPer100g;

    @Column(name = "calories_per_unit", precision = 10, scale = 2)
    private BigDecimal caloriesPerUnit;

    @Column(name = "protein_per_unit", precision = 10, scale = 2)
    private BigDecimal proteinPerUnit;

    @Column(name = "carbohydrates_per_unit", precision = 10, scale = 2)
    private BigDecimal carbohydratesPerUnit;

    @Column(name = "fat_per_unit", precision = 10, scale = 2)
    private BigDecimal fatPerUnit;

    @Column(name = "fiber_per_unit", precision = 10, scale = 2)
    private BigDecimal fiberPerUnit;

    @Column(name = "sugar_per_unit", precision = 10, scale = 2)
    private BigDecimal sugarPerUnit;

    @Column(name = "salt_per_unit", precision = 10, scale = 2)
    private BigDecimal saltPerUnit;

    @Column(name = "data_source", nullable = false, length = 80)
    private String dataSource;

    @Column(name = "verification_status", nullable = false, length = 40)
    private String verificationStatus;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "nutrition_basis", nullable = false, length = 40)
    private String nutritionBasis = "PER_100_GRAMS";

    @Column(nullable = false, length = 20)
    private String completeness = "EMPTY";

    @Column(name = "source_reference", length = 500)
    private String sourceReference;

    @Column(name = "source_updated_at")
    private OffsetDateTime sourceUpdatedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected NutritionEntity() {
    }

    public NutritionEntity(ProductEntity product) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.createdAt = OffsetDateTime.now();
    }

    public void update(
            BigDecimal caloriesPer100g,
            BigDecimal proteinPer100g,
            BigDecimal carbohydratesPer100g,
            BigDecimal fatPer100g,
            BigDecimal fiberPer100g,
            BigDecimal sugarPer100g,
            BigDecimal saltPer100g,
            BigDecimal caloriesPerUnit,
            BigDecimal proteinPerUnit,
            BigDecimal carbohydratesPerUnit,
            BigDecimal fatPerUnit,
            BigDecimal fiberPerUnit,
            BigDecimal sugarPerUnit,
            BigDecimal saltPerUnit,
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
        this.caloriesPerUnit = caloriesPerUnit;
        this.proteinPerUnit = proteinPerUnit;
        this.carbohydratesPerUnit = carbohydratesPerUnit;
        this.fatPerUnit = fatPerUnit;
        this.fiberPerUnit = fiberPerUnit;
        this.sugarPerUnit = sugarPerUnit;
        this.saltPerUnit = saltPerUnit;
        this.dataSource = dataSource;
        this.verificationStatus = verificationStatus;
        this.confidenceScore = normalizeLegacyConfidence(confidenceScore);
        this.nutritionBasis = "PER_100_GRAMS";
        this.completeness = calculateCompleteness(caloriesPer100g, proteinPer100g,
                carbohydratesPer100g, fatPer100g);
        this.sourceUpdatedAt = updatedAt;
        this.updatedAt = updatedAt;
    }

    public void apply(NutritionValues values, String dataSource, String verificationStatus,
            BigDecimal confidenceScore, String sourceReference, OffsetDateTime sourceUpdatedAt,
            UUID reviewedBy, OffsetDateTime reviewedAt, OffsetDateTime updatedAt) {
        this.caloriesPer100g = values.calories();
        this.proteinPer100g = values.protein();
        this.carbohydratesPer100g = values.carbohydrates();
        this.fatPer100g = values.fat();
        this.fiberPer100g = values.fiber();
        this.sugarPer100g = values.sugars();
        this.saltPer100g = values.salt();
        this.saturatedFatPer100g = values.saturatedFat();
        this.nutritionBasis = values.basis();
        this.completeness = calculateCompleteness(values.calories(), values.protein(),
                values.carbohydrates(), values.fat());
        this.dataSource = dataSource;
        this.verificationStatus = verificationStatus;
        this.confidenceScore = confidenceScore;
        this.sourceReference = sourceReference;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.updatedAt = updatedAt;
    }

    private static BigDecimal normalizeLegacyConfidence(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ONE) <= 0
                ? value.multiply(BigDecimal.valueOf(100)) : value;
    }

    private static String calculateCompleteness(BigDecimal calories, BigDecimal protein,
            BigDecimal carbohydrates, BigDecimal fat) {
        int present = 0;
        if (calories != null) present++;
        if (protein != null) present++;
        if (carbohydrates != null) present++;
        if (fat != null) present++;
        return switch (present) { case 4 -> "COMPLETE"; case 2, 3 -> "PARTIAL";
            case 1 -> "MINIMAL"; default -> "EMPTY"; };
    }

    public record NutritionValues(String basis, BigDecimal calories, BigDecimal protein,
            BigDecimal carbohydrates, BigDecimal fat, BigDecimal fiber, BigDecimal sugars,
            BigDecimal salt, BigDecimal saturatedFat) {}

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

    public BigDecimal getSaturatedFatPer100g() { return saturatedFatPer100g; }

    public BigDecimal getCaloriesPerUnit() {
        return caloriesPerUnit;
    }

    public BigDecimal getProteinPerUnit() {
        return proteinPerUnit;
    }

    public BigDecimal getCarbohydratesPerUnit() {
        return carbohydratesPerUnit;
    }

    public BigDecimal getFatPerUnit() {
        return fatPerUnit;
    }

    public BigDecimal getFiberPerUnit() {
        return fiberPerUnit;
    }

    public BigDecimal getSugarPerUnit() {
        return sugarPerUnit;
    }

    public BigDecimal getSaltPerUnit() {
        return saltPerUnit;
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

    public String getNutritionBasis() { return nutritionBasis; }
    public String getCompleteness() { return completeness; }
    public String getSourceReference() { return sourceReference; }
    public OffsetDateTime getSourceUpdatedAt() { return sourceUpdatedAt; }
    public UUID getReviewedBy() { return reviewedBy; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public long getRowVersion() { return rowVersion; }
}
