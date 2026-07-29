package com.sean.supermarketmealplanner.activity.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.sean.supermarketmealplanner.activity.domain.ActivityOrigin;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        String type,
        String summary,
        OffsetDateTime occurredAt,
        ActivityOrigin origin,
        String resourceType,
        UUID resourceId,
        String link,
        JsonNode deltas
) {
}
