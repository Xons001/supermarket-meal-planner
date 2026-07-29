package com.sean.supermarketmealplanner.activity.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityEventRepository extends JpaRepository<UserActivityEventEntity, UUID> {
}
