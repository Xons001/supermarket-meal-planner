package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import com.sean.supermarketmealplanner.catalog.domain.PresenceType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_allergens")
public class ProductAllergenEntity {

    @EmbeddedId
    private ProductAllergenId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("allergenId")
    @JoinColumn(name = "allergen_id")
    private AllergenEntity allergen;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(name = "presence_type", nullable = false, length = 30)
    private PresenceType presenceType;

    protected ProductAllergenEntity() {
    }

    public ProductAllergenEntity(
            ProductEntity product,
            AllergenEntity allergen,
            PresenceType presenceType
    ) {
        this.id = new ProductAllergenId(product.getId(), allergen.getId());
        this.product = product;
        this.allergen = allergen;
        this.presenceType = presenceType;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public AllergenEntity getAllergen() {
        return allergen;
    }

    public PresenceType getPresenceType() {
        return presenceType;
    }
}
