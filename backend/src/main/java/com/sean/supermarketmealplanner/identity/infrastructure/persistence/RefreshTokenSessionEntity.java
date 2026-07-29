package com.sean.supermarketmealplanner.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_token_sessions")
public class RefreshTokenSessionEntity {
    @Id private UUID id;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "family_id", nullable = false) private UUID familyId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private UserAccountEntity user;
    @Column(name = "expires_at", nullable = false) private OffsetDateTime expiresAt;
    @Column(name = "rotated_at") private OffsetDateTime rotatedAt;
    @Column(name = "revoked_at") private OffsetDateTime revokedAt;
    @Column(name = "replaced_by") private UUID replacedBy;
    @Column(name = "user_agent", length = 255) private String userAgent;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;

    protected RefreshTokenSessionEntity() {}
    public RefreshTokenSessionEntity(UUID id, String hash, UUID familyId, UserAccountEntity user,
                                     OffsetDateTime expiresAt, String userAgent, OffsetDateTime now) {
        this.id = id; this.tokenHash = hash; this.familyId = familyId; this.user = user;
        this.expiresAt = expiresAt; this.userAgent = userAgent == null ? null :
                userAgent.substring(0, Math.min(255, userAgent.length())); this.createdAt = now;
    }
    public void rotateTo(UUID replacement, OffsetDateTime now) {
        this.rotatedAt = now; this.revokedAt = now; this.replacedBy = replacement;
    }
    public void revoke(OffsetDateTime now) { if (revokedAt == null) revokedAt = now; }
    public UUID getId() { return id; }
    public UUID getFamilyId() { return familyId; }
    public UserAccountEntity getUser() { return user; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public UUID getReplacedBy() { return replacedBy; }
    public boolean isUsableAt(OffsetDateTime now) { return revokedAt == null && expiresAt.isAfter(now); }
}
