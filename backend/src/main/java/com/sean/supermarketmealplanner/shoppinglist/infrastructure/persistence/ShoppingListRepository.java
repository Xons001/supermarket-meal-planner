package com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShoppingListRepository extends JpaRepository<ShoppingListEntity, UUID>,
        JpaSpecificationExecutor<ShoppingListEntity> {

    Optional<ShoppingListEntity> findByMealPlanIdAndActiveTrue(UUID mealPlanId);

    boolean existsByMealPlanIdAndActiveTrue(UUID mealPlanId);
    Optional<ShoppingListEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    Optional<ShoppingListEntity> findByMealPlanIdAndOwnerIdAndActiveTrue(UUID mealPlanId, UUID ownerId);
    java.util.List<ShoppingListEntity> findAllByOwnerId(UUID ownerId);
}
