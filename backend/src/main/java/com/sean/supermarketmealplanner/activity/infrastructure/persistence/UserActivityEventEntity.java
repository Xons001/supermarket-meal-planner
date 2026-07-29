package com.sean.supermarketmealplanner.activity.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.sean.supermarketmealplanner.activity.domain.ActivityOrigin;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_activity_events")
public class UserActivityEventEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccountEntity owner;
    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;
    @Column(nullable = false, length = 500)
    private String summary;
    @Column(name = "resource_type", nullable = false, length = 30)
    private String resourceType;
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;
    @Column(name = "secondary_resource_id")
    private UUID secondaryResourceId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode metadata;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityOrigin origin;
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected UserActivityEventEntity() {}

    public UserActivityEventEntity(UserAccountEntity owner, String eventType, String summary,
            String resourceType, UUID resourceId, UUID secondaryResourceId, JsonNode metadata,
            OffsetDateTime now) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.eventType = eventType;
        this.summary = summary;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.secondaryResourceId = secondaryResourceId;
        this.metadata = metadata;
        this.origin = ActivityOrigin.LIVE;
        this.occurredAt = now;
        this.createdAt = now;
    }
}
