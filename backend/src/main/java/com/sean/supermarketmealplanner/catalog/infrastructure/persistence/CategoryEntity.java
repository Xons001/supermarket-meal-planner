package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoryEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supermarket_id", nullable = false)
    private SupermarketEntity supermarket;

    @Column(name = "external_id", nullable = false, length = 160)
    private String externalId;

    @Column(nullable = false, length = 160)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private CategoryEntity parentCategory;

    @Column(nullable = false)
    private boolean active;

    protected CategoryEntity() {
    }

    public CategoryEntity(SupermarketEntity supermarket, String externalId, String name) {
        this.id = UUID.randomUUID();
        this.supermarket = supermarket;
        this.externalId = externalId;
        this.name = name;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public SupermarketEntity getSupermarket() {
        return supermarket;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public CategoryEntity getParentCategory() {
        return parentCategory;
    }

    public boolean isActive() {
        return active;
    }

    public void update(String name, CategoryEntity parentCategory, boolean active) {
        this.name = name;
        this.parentCategory = parentCategory;
        this.active = active;
    }
}
