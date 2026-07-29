package com.sean.supermarketmealplanner.mealplan.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MealPlanRepository extends
        JpaRepository<MealPlanEntity, UUID>,
        JpaSpecificationExecutor<MealPlanEntity> {

    Optional<MealPlanEntity> findByIdAndArchivedFalse(UUID id);
    Optional<MealPlanEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
