package com.sean.supermarketmealplanner.mealtemplate.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealTemplateRepository extends JpaRepository<MealTemplateEntity, UUID> {

    List<MealTemplateEntity> findAllByArchivedFalse();

    Optional<MealTemplateEntity> findByIdAndArchivedFalse(UUID id);

    Optional<MealTemplateEntity> findBySupermarketIdAndNameIgnoreCase(
            UUID supermarketId,
            String name
    );
}
