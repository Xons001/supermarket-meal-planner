package com.sean.supermarketmealplanner.nutrition.infrastructure.persistence;
import com.sean.supermarketmealplanner.nutrition.domain.NutritionEnums.EnrichmentStatus;
import java.util.*;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface NutritionEnrichmentRunRepository extends JpaRepository<NutritionEnrichmentRunEntity,UUID>{
    boolean existsByStatusIn(Collection<EnrichmentStatus> statuses); Page<NutritionEnrichmentRunEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
