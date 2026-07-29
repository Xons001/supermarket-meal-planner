package com.sean.supermarketmealplanner.identity.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSessionEntity, UUID> {
    Optional<RefreshTokenSessionEntity> findByTokenHash(String tokenHash);
    @EntityGraph(attributePaths = "user")
    @Query("select s from RefreshTokenSessionEntity s where s.id = :id")
    Optional<RefreshTokenSessionEntity> findWithUserById(UUID id);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshTokenSessionEntity s set s.revokedAt = :now where s.user.id = :userId and s.revokedAt is null")
    int revokeAllByUserId(UUID userId, OffsetDateTime now);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshTokenSessionEntity s set s.revokedAt = :now where s.familyId = :familyId and s.revokedAt is null")
    int revokeFamily(UUID familyId, OffsetDateTime now);
}
