package com.sean.supermarketmealplanner.mealplan.infrastructure.persistence;

import com.sean.supermarketmealplanner.mealplan.domain.MealPlanChangeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealPlanChangeRepository extends JpaRepository<MealPlanChangeEntity, UUID> {
    List<MealPlanChangeEntity> findByMealPlanIdOrderBySequenceNumberDesc(UUID mealPlanId);
    Optional<MealPlanChangeEntity> findFirstByMealPlanIdAndChangeTypeInAndUndoneByChangeIdIsNullOrderBySequenceNumberDesc(
            UUID mealPlanId,
            Set<MealPlanChangeType> types
    );
    long countByMealPlanId(UUID mealPlanId);
}
