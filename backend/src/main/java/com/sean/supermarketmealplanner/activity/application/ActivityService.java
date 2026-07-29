package com.sean.supermarketmealplanner.activity.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.supermarketmealplanner.activity.domain.ActivityOrigin;
import com.sean.supermarketmealplanner.activity.infrastructure.persistence.UserActivityEventEntity;
import com.sean.supermarketmealplanner.activity.infrastructure.persistence.UserActivityEventRepository;
import com.sean.supermarketmealplanner.identity.application.CurrentUserProvider;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserAccountEntity;
import com.sean.supermarketmealplanner.shared.application.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private static final String UNION = """
            SELECT id, event_type, summary, occurred_at, origin, resource_type, resource_id,
                   metadata AS deltas
            FROM user_activity_events
            WHERE owner_id = ?
            UNION ALL
            SELECT c.id, c.change_type,
                   c.reason, c.created_at, 'LIVE', 'MEAL_PLAN', c.meal_plan_id,
                   c.metrics_delta
            FROM meal_plan_changes c
            JOIN meal_plans p ON p.id = c.meal_plan_id
            WHERE p.owner_id = ?
            """;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final UserActivityEventRepository events;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public ActivityService(JdbcTemplate jdbc, ObjectMapper json, UserActivityEventRepository events,
            CurrentUserProvider currentUser, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.events = events;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> findAll(String type, int page, int size) {
        var safePage = Math.max(0, page);
        var safeSize = Math.min(100, Math.max(1, size));
        var filtered = "SELECT * FROM (" + UNION + ") activity "
                + "WHERE (CAST(? AS VARCHAR) IS NULL OR event_type = ?) ";
        var rows = jdbc.query(filtered + "ORDER BY occurred_at DESC, id DESC LIMIT ? OFFSET ?",
                this::map, currentUser.userId(), currentUser.userId(), type, type,
                safeSize, safePage * safeSize);
        var total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + filtered + ") counted",
                Long.class, currentUser.userId(), currentUser.userId(), type, type);
        var pages = total == null || total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new PageResponse<>(rows, safePage, safeSize, total == null ? 0 : total, pages,
                safePage == 0, safePage + 1 >= pages);
    }

    @Transactional
    public void record(UserAccountEntity owner, String type, String summary, String resourceType,
            UUID resourceId, UUID secondaryResourceId, Map<String, ?> metadata) {
        events.save(new UserActivityEventEntity(owner, type, summary, resourceType, resourceId,
                secondaryResourceId, json.valueToTree(metadata), OffsetDateTime.now(clock)));
    }

    private ActivityResponse map(ResultSet rs, int row) throws SQLException {
        var resourceType = rs.getString("resource_type");
        var resourceId = rs.getObject("resource_id", UUID.class);
        return new ActivityResponse(rs.getObject("id", UUID.class), rs.getString("event_type"),
                rs.getString("summary"), rs.getObject("occurred_at", OffsetDateTime.class),
                ActivityOrigin.valueOf(rs.getString("origin")), resourceType, resourceId,
                "SHOPPING_LIST".equals(resourceType) ? "/shopping-lists/" + resourceId
                        : "/meal-plans/" + resourceId,
                readJson(rs.getString("deltas")));
    }

    private JsonNode readJson(String value) {
        try {
            return value == null ? json.createObjectNode() : json.readTree(value);
        } catch (JsonProcessingException exception) {
            return json.createObjectNode();
        }
    }
}
