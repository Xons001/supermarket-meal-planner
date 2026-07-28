package com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.mealtemplate.domain.QuantityUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "meal_template_ingredients")
public class MealTemplateIngredientEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_template_id", nullable = false)
    private MealTemplateEntity mealTemplate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit", nullable = false, length = 20)
    private QuantityUnit quantityUnit;

    @Column(nullable = false)
    private boolean optional;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(length = 500)
    private String notes;

    protected MealTemplateIngredientEntity() {
    }

    MealTemplateIngredientEntity(
            MealTemplateEntity mealTemplate,
            ProductEntity product,
            BigDecimal quantity,
            QuantityUnit quantityUnit,
            boolean optional,
            int sortOrder,
            String notes
    ) {
        this.id = UUID.randomUUID();
        this.mealTemplate = mealTemplate;
        this.product = product;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.optional = optional;
        this.sortOrder = sortOrder;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public QuantityUnit getQuantityUnit() {
        return quantityUnit;
    }

    public boolean isOptional() {
        return optional;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getNotes() {
        return notes;
    }
}
