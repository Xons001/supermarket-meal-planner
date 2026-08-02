package com.sean.supermarketmealplanner.identity.application;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDataExportService {
    private final IdentityService identity;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Clock clock;
    private final IdentityAuditLogger audit;

    public UserDataExportService(IdentityService identity, JdbcTemplate jdbc, ObjectMapper json,
                                 Clock clock, IdentityAuditLogger audit) {
        this.identity = identity; this.jdbc = jdbc; this.json = json; this.clock = clock; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public void write(UUID userId, OutputStream output) throws IOException {
        try (JsonGenerator generator = json.getFactory().createGenerator(output)) {
            generator.writeStartObject();
            generator.writeStringField("exportedAt", OffsetDateTime.now(clock).toString());
            generator.writeObjectField("profile", identity.me(userId));
            writeJsonRows(generator, "mealPlans", """
                    select jsonb_build_object(
                      'id', id, 'name', name, 'status', status, 'favorite', favorite,
                      'archivedAt', archived_at, 'createdAt', created_at,
                      'content', result_json::jsonb
                    )::text
                    from meal_plans where owner_id=? order by created_at
                    """, userId);
            writeJsonRows(generator, "shoppingLists", """
                    select jsonb_build_object(
                      'id', sl.id, 'mealPlanId', sl.meal_plan_id, 'status', sl.status,
                      'active', sl.active, 'archivedAt', sl.archived_at,
                      'createdAt', sl.created_at, 'summary', sl.quantity_summary_json::jsonb,
                      'items', coalesce((select jsonb_agg(to_jsonb(i) - 'shopping_list_id' order by i.product_name_snapshot)
                                         from shopping_list_items i where i.shopping_list_id=sl.id), '[]'::jsonb)
                    )::text
                    from shopping_lists sl where sl.owner_id=? order by sl.created_at
                    """, userId);
            writeJsonRows(generator, "activity", """
                    select jsonb_build_object(
                      'type', event_type, 'summary', summary, 'resourceType', resource_type,
                      'resourceId', resource_id, 'metadata', metadata, 'origin', origin,
                      'occurredAt', occurred_at
                    )::text
                    from user_activity_events where owner_id=? order by occurred_at
                    """, userId);
            generator.writeEndObject();
        }
        audit.success("data_exported", userId);
    }

    private void writeJsonRows(JsonGenerator generator, String field, String sql, UUID userId) throws IOException {
        generator.writeArrayFieldStart(field);
        try {
            jdbc.query(connection -> {
                var statement = connection.prepareStatement(sql);
                statement.setObject(1, userId);
                statement.setFetchSize(100);
                return statement;
            }, result -> {
                try {
                    generator.writeTree(json.readTree(result.getString(1)));
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
        generator.writeEndArray();
    }
}
