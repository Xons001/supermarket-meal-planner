package com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.shoppinglist.application.ShoppingListResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shopping_list_items")
public class ShoppingListItemEntity {

    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingListEntity shoppingList;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "category_id")
    private UUID categoryId;
    @Column(name = "product_name_snapshot", nullable = false, length = 240)
    private String productName;
    @Column(name = "brand_snapshot", length = 160)
    private String brand;
    @Column(name = "category_name_snapshot", length = 160)
    private String categoryName;
    @Column(name = "measurement_type", length = 20)
    private String measurementType;
    @Column(name = "required_unit", nullable = false, length = 30)
    private String requiredUnit;
    @Column(name = "package_quantity_snapshot", precision = 14, scale = 3)
    private BigDecimal packageQuantity;
    @Column(name = "package_unit_snapshot", length = 30)
    private String packageUnit;
    @Column(name = "package_price_snapshot", precision = 14, scale = 2)
    private BigDecimal packagePrice;
    @Column(name = "unit_price_snapshot", precision = 14, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "required_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal requiredQuantity;
    @Column(name = "packages_required")
    private Integer packagesRequired;
    @Column(name = "purchased_quantity", precision = 14, scale = 3)
    private BigDecimal purchasedQuantity;
    @Column(name = "leftover_quantity", precision = 14, scale = 3)
    private BigDecimal leftoverQuantity;
    @Column(name = "consumed_cost", precision = 14, scale = 2)
    private BigDecimal consumedCost;
    @Column(name = "purchase_cost", precision = 14, scale = 2)
    private BigDecimal purchaseCost;
    @Column(name = "waste_cost", precision = 14, scale = 2)
    private BigDecimal wasteCost;
    @Column(name = "leftover_percentage", precision = 6, scale = 1)
    private BigDecimal leftoverPercentage;
    @Column(name = "available_snapshot")
    private Boolean available;
    @Column(name = "calculation_complete", nullable = false)
    private boolean calculationComplete;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "warnings_snapshot", nullable = false, columnDefinition = "TEXT")
    private String warningsJson;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ShoppingListItemEntity() {
    }

    ShoppingListItemEntity(
            ShoppingListEntity shoppingList,
            ShoppingListResponse.Item item,
            OffsetDateTime now
    ) {
        this.id = item.id();
        this.shoppingList = shoppingList;
        this.productId = item.productId();
        this.categoryId = item.categoryId();
        this.productName = item.productName();
        this.brand = item.brand();
        this.categoryName = item.categoryName();
        this.measurementType = item.measurementType();
        this.requiredUnit = item.requiredUnit();
        this.packageQuantity = item.packageQuantity();
        this.packageUnit = item.packageUnit();
        this.packagePrice = item.packagePrice();
        this.unitPrice = item.unitPrice();
        this.requiredQuantity = item.requiredQuantity();
        this.packagesRequired = item.packagesRequired();
        this.purchasedQuantity = item.purchasedQuantity();
        this.leftoverQuantity = item.leftoverQuantity();
        this.consumedCost = item.consumedCost();
        this.purchaseCost = item.purchaseCost();
        this.wasteCost = item.wasteCost();
        this.leftoverPercentage = item.leftoverPercentage();
        this.available = item.available();
        this.calculationComplete = item.calculationComplete();
        this.sortOrder = item.sortOrder();
        this.warningsJson = json(item.warnings());
        this.createdAt = now;
        this.updatedAt = now;
    }

    private String json(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize item warnings", exception);
        }
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public UUID getCategoryId() { return categoryId; }
    public String getProductName() { return productName; }
    public String getBrand() { return brand; }
    public String getCategoryName() { return categoryName; }
    public String getMeasurementType() { return measurementType; }
    public String getRequiredUnit() { return requiredUnit; }
    public BigDecimal getPackageQuantity() { return packageQuantity; }
    public String getPackageUnit() { return packageUnit; }
    public BigDecimal getPackagePrice() { return packagePrice; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getRequiredQuantity() { return requiredQuantity; }
    public Integer getPackagesRequired() { return packagesRequired; }
    public BigDecimal getPurchasedQuantity() { return purchasedQuantity; }
    public BigDecimal getLeftoverQuantity() { return leftoverQuantity; }
    public BigDecimal getConsumedCost() { return consumedCost; }
    public BigDecimal getPurchaseCost() { return purchaseCost; }
    public BigDecimal getWasteCost() { return wasteCost; }
    public BigDecimal getLeftoverPercentage() { return leftoverPercentage; }
    public Boolean getAvailable() { return available; }
    public boolean isCalculationComplete() { return calculationComplete; }
    public int getSortOrder() { return sortOrder; }
    public String getWarningsJson() { return warningsJson; }
}
