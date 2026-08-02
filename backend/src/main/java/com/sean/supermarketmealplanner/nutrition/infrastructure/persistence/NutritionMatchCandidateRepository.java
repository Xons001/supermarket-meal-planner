package com.sean.supermarketmealplanner.nutrition.infrastructure.persistence;
import com.sean.supermarketmealplanner.nutrition.domain.NutritionEnums.CandidateStatus;
import java.util.*;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.*;
public interface NutritionMatchCandidateRepository extends JpaRepository<NutritionMatchCandidateEntity,UUID>{
    @EntityGraph(attributePaths={"product","product.nutrition","run"})
    Page<NutritionMatchCandidateEntity> findByStatusOrderByConfidenceScoreDescCreatedAtAsc(CandidateStatus status,Pageable pageable);
    @Override @EntityGraph(attributePaths={"product","product.nutrition","run"}) Optional<NutritionMatchCandidateEntity> findById(UUID id);
}
