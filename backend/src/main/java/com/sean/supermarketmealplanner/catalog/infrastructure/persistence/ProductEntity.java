package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import com.sean.supermarketmealplanner.catalog.domain.PackageUnit;
import com.sean.supermarketmealplanner.catalog.domain.MeasurementType;
import com.sean.supermarketmealplanner.nutrition.infrastructure.persistence.NutritionEntity;
import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supermarket_id", nullable = false)
    private SupermarketEntity supermarket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(name = "external_id", nullable = false, length = 160)
    private String externalId;

    @Column(length = 64)
    private String barcode;

    @Column(nullable = false, length = 240)
    private String name;

    @Column(length = 160)
    private String brand;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "product_url", length = 1000)
    private String productUrl;

    @Column(name = "current_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "package_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal packageQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_unit", nullable = false, length = 30)
    private PackageUnit packageUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_type", nullable = false, length = 20)
    private MeasurementType measurementType;

    @Column(name = "cost_data_complete", nullable = false)
    private boolean costDataComplete;

    @Column(nullable = false)
    private boolean available;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "last_synced_at", nullable = false)
    private OffsetDateTime lastSyncedAt;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "unavailable_since")
    private OffsetDateTime unavailableSince;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY)
    private NutritionEntity nutrition;

    protected ProductEntity() {
    }

    public ProductEntity(SupermarketEntity supermarket, CategoryEntity category, String externalId) {
        var now = OffsetDateTime.now();
        this.id = UUID.randomUUID();
        this.supermarket = supermarket;
        this.category = category;
        this.externalId = externalId;
        this.createdAt = now;
        this.updatedAt = now;
        this.lastSyncedAt = now;
        this.lastSeenAt = now;
    }

    public void update(
            CategoryEntity category,
            String barcode,
            String name,
            String brand,
            String description,
            BigDecimal currentPrice,
            BigDecimal unitPrice,
            BigDecimal packageQuantity,
            PackageUnit packageUnit,
            MeasurementType measurementType,
            boolean costDataComplete,
            boolean available,
            String source,
            OffsetDateTime syncedAt
    ) {
        this.category = category;
        this.barcode = barcode;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.currentPrice = currentPrice;
        this.unitPrice = unitPrice;
        this.packageQuantity = packageQuantity;
        this.packageUnit = packageUnit;
        this.measurementType = measurementType;
        this.costDataComplete = costDataComplete;
        this.available = available;
        this.source = source;
        this.lastSyncedAt = syncedAt;
        this.lastSeenAt = syncedAt;
        this.unavailableSince = available ? null : (this.unavailableSince == null ? syncedAt : this.unavailableSince);
        this.updatedAt = syncedAt;
    }

    public void markUnavailable(OffsetDateTime syncedAt) {
        this.available = false;
        this.unavailableSince = this.unavailableSince == null ? syncedAt : this.unavailableSince;
        this.lastSyncedAt = syncedAt;
        this.updatedAt = syncedAt;
    }

    public UUID getId() {
        return id;
    }

    public SupermarketEntity getSupermarket() {
        return supermarket;
    }

    public CategoryEntity getCategory() {
        return category;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getPackageQuantity() {
        return packageQuantity;
    }

    public PackageUnit getPackageUnit() {
        return packageUnit;
    }

    public MeasurementType getMeasurementType() {
        return measurementType;
    }

    public boolean isCostDataComplete() {
        return costDataComplete;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getSource() {
        return source;
    }

    public OffsetDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }

    public OffsetDateTime getUnavailableSince() { return unavailableSince; }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public NutritionEntity getNutrition() {
        return nutrition;
    }

    public void setNutrition(NutritionEntity nutrition) {
        this.nutrition = nutrition;
    }
}
