package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ProductDietaryTagId implements Serializable {

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "dietary_tag_id")
    private UUID dietaryTagId;

    protected ProductDietaryTagId() {
    }

    public ProductDietaryTagId(UUID productId, UUID dietaryTagId) {
        this.productId = productId;
        this.dietaryTagId = dietaryTagId;
    }

    public UUID getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProductDietaryTagId that)) {
            return false;
        }
        return Objects.equals(productId, that.productId)
                && Objects.equals(dietaryTagId, that.dietaryTagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, dietaryTagId);
    }
}
