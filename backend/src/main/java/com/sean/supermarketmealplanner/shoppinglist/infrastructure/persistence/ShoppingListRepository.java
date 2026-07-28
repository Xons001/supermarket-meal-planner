package com.sean.supermarketmealplanner.shoppinglist.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingListRepository extends JpaRepository<ShoppingListEntity, UUID> {

    Optional<ShoppingListEntity> findByMealPlanIdAndArchivedFalse(UUID mealPlanId);

    boolean existsByMealPlanIdAndArchivedFalse(UUID mealPlanId);
}
