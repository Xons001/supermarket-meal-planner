package com.sean.supermarketmealplanner.catalog.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DietaryTagRepository extends JpaRepository<DietaryTagEntity, UUID> {

    Optional<DietaryTagEntity> findByCode(String code);

    List<DietaryTagEntity> findAllByCodeIn(Collection<String> codes);

    List<DietaryTagEntity> findAllByOrderByNameAsc();
}
