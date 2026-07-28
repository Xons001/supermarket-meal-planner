package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import com.sean.supermarketmealplanner.catalog.application.ProductSearchCriteria;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<ProductEntity> matching(ProductSearchCriteria criteria) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();

            if (criteria.supermarketCode() != null) {
                predicates.add(builder.equal(
                        root.get("supermarket").get("code"),
                        criteria.supermarketCode()
                ));
            }
            if (criteria.categoryId() != null) {
                predicates.add(builder.equal(root.get("category").get("id"), criteria.categoryId()));
            }
            if (criteria.query() != null) {
                var pattern = "%" + escapeLike(criteria.query().toLowerCase()) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern, '\\'),
                        builder.like(builder.lower(root.get("brand")), pattern, '\\')
                ));
            }
            if (criteria.available() != null) {
                predicates.add(builder.equal(root.get("available"), criteria.available()));
            }
            if (criteria.maximumPrice() != null) {
                predicates.add(builder.lessThanOrEqualTo(
                        root.get("currentPrice"),
                        criteria.maximumPrice()
                ));
            }
            if (criteria.maximumCalories() != null || criteria.minimumProtein() != null) {
                var nutrition = root.join("nutrition", JoinType.INNER);
                if (criteria.maximumCalories() != null) {
                    predicates.add(builder.lessThanOrEqualTo(
                            nutrition.get("caloriesPer100g"),
                            criteria.maximumCalories()
                    ));
                }
                if (criteria.minimumProtein() != null) {
                    predicates.add(builder.greaterThanOrEqualTo(
                            nutrition.get("proteinPer100g"),
                            criteria.minimumProtein()
                    ));
                }
            }
            for (var tagCode : criteria.dietaryTags()) {
                var tagExists = query.subquery(Integer.class);
                var productTag = tagExists.from(ProductDietaryTagEntity.class);
                tagExists.select(builder.literal(1)).where(
                        builder.equal(productTag.get("product"), root),
                        builder.equal(productTag.get("dietaryTag").get("code"), tagCode)
                );
                predicates.add(builder.exists(tagExists));
            }
            if (!criteria.excludedAllergens().isEmpty()) {
                var allergenExists = query.subquery(Integer.class);
                var productAllergen = allergenExists.from(ProductAllergenEntity.class);
                allergenExists.select(builder.literal(1)).where(
                        builder.equal(productAllergen.get("product"), root),
                        productAllergen.get("allergen").get("code")
                                .in(criteria.excludedAllergens())
                );
                predicates.add(builder.not(builder.exists(allergenExists)));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
