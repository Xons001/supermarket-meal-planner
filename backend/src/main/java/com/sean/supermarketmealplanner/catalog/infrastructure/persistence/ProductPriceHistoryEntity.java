package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_price_history")
public class ProductPriceHistoryEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "sync_run_id")
    private UUID syncRunId;

    @Column(length = 50)
    private String source;

    protected ProductPriceHistoryEntity() {
    }

    public ProductPriceHistoryEntity(
            ProductEntity product,
            BigDecimal price,
            BigDecimal unitPrice,
            OffsetDateTime recordedAt
    ) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.price = price;
        this.unitPrice = unitPrice;
        this.recordedAt = recordedAt;
        this.source = "LOCAL_JSON";
    }

    public UUID getId() {
        return id;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public UUID getSyncRunId() { return syncRunId; }

    public String getSource() { return source; }
}
