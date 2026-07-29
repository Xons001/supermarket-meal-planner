package com.sean.supermarketmealplanner.identity.infrastructure.persistence;

import com.sean.supermarketmealplanner.identity.domain.UserRole;
import com.sean.supermarketmealplanner.identity.domain.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccountEntity {
    @Id
    private UUID id;
    @Column(name = "normalized_email", nullable = false, unique = true, length = 320)
    private String normalizedEmail;
    @Column(name = "password_hash", length = 255)
    private String passwordHash;
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role;
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected UserAccountEntity() {}

    public UserAccountEntity(String email, String passwordHash, String displayName, UserRole role, OffsetDateTime now) {
        this.id = UUID.randomUUID();
        this.normalizedEmail = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.displayName = displayName.strip();
        this.status = UserStatus.ACTIVE;
        this.role = role;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT);
    }

    public void recordLogin(OffsetDateTime now) { this.lastLoginAt = now; this.updatedAt = now; }
    public void updateProfile(String name, OffsetDateTime now) { this.displayName = name.strip(); this.updatedAt = now; }
    public void changePassword(String hash, OffsetDateTime now) { this.passwordHash = hash; this.updatedAt = now; }
    public void disable(OffsetDateTime now) { this.status = UserStatus.DISABLED; this.updatedAt = now; }
    public UUID getId() { return id; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public UserStatus getStatus() { return status; }
    public UserRole getRole() { return role; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
