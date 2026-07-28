package com.sean.supermarketmealplanner.supermarket.infrastructure.persistence;

import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupermarketRepository extends JpaRepository<SupermarketEntity, UUID> {

    Optional<SupermarketEntity> findByCode(SupermarketCode code);
}
