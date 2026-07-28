package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ProductAllergenId implements Serializable {

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "allergen_id")
    private UUID allergenId;

    protected ProductAllergenId() {
    }

    public ProductAllergenId(UUID productId, UUID allergenId) {
        this.productId = productId;
        this.allergenId = allergenId;
    }

    public UUID getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProductAllergenId that)) {
            return false;
        }
        return Objects.equals(productId, that.productId)
                && Objects.equals(allergenId, that.allergenId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, allergenId);
    }
}
