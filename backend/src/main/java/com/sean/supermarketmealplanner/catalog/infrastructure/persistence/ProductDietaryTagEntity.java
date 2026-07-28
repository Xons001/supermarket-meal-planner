package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_dietary_tags")
public class ProductDietaryTagEntity {

    @EmbeddedId
    private ProductDietaryTagId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("dietaryTagId")
    @JoinColumn(name = "dietary_tag_id")
    private DietaryTagEntity dietaryTag;

    protected ProductDietaryTagEntity() {
    }

    public ProductDietaryTagEntity(ProductEntity product, DietaryTagEntity dietaryTag) {
        this.id = new ProductDietaryTagId(product.getId(), dietaryTag.getId());
        this.product = product;
        this.dietaryTag = dietaryTag;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public DietaryTagEntity getDietaryTag() {
        return dietaryTag;
    }
}
