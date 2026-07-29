package com.sean.supermarketmealplanner.dashboard.application;

import com.sean.supermarketmealplanner.activity.application.ActivityService;
import com.sean.supermarketmealplanner.identity.application.CurrentUserProvider;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUser;
    private final ActivityService activity;

    public DashboardService(JdbcTemplate jdbc, CurrentUserProvider currentUser, ActivityService activity) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get() {
        var owner = currentUser.userId();
        var metrics = jdbc.queryForObject("""
                SELECT
                  COUNT(*) FILTER (WHERE NOT archived) AS active_plans,
                  COUNT(*) FILTER (WHERE favorite) AS favorite_plans,
                  (SELECT COUNT(*) FROM shopping_lists sl WHERE sl.owner_id = ?) AS shopping_lists,
                  (SELECT COUNT(*) FROM shopping_lists sl JOIN meal_plans p ON p.id = sl.meal_plan_id
                     WHERE sl.owner_id = ? AND sl.active AND NOT sl.archived
                       AND sl.source_plan_content_version = p.content_version) AS current_lists,
                  (SELECT COUNT(*) FROM shopping_lists sl JOIN meal_plans p ON p.id = sl.meal_plan_id
                     WHERE sl.owner_id = ? AND sl.active AND NOT sl.archived
                       AND sl.source_plan_content_version <> p.content_version) AS outdated_lists,
                  AVG(estimated_purchase_cost) FILTER (WHERE NOT archived) AS average_purchase,
                  AVG(estimated_waste_cost) FILTER (WHERE NOT archived) AS average_waste
                FROM meal_plans WHERE owner_id = ?
                """, (rs, row) -> new DashboardResponse.Metrics(
                        rs.getLong("active_plans"), rs.getLong("favorite_plans"),
                        rs.getLong("shopping_lists"), rs.getLong("current_lists"),
                        rs.getLong("outdated_lists"), money(rs.getBigDecimal("average_purchase")),
                        money(rs.getBigDecimal("average_waste"))),
                owner, owner, owner, owner);
        var plans = jdbc.query("""
                SELECT id, name, start_date, generation_strategy, overall_score,
                       estimated_purchase_cost, estimated_waste_cost, favorite, updated_at
                FROM meal_plans
                WHERE owner_id = ? AND NOT archived
                ORDER BY updated_at DESC, id DESC
                LIMIT 1
                """, (rs, row) -> new DashboardResponse.PlanCard(
                        rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getObject("start_date", java.time.LocalDate.class),
                        rs.getString("generation_strategy"), rs.getBigDecimal("overall_score"),
                        rs.getBigDecimal("estimated_purchase_cost"), rs.getBigDecimal("estimated_waste_cost"),
                        rs.getBoolean("favorite"),
                        rs.getObject("updated_at", java.time.OffsetDateTime.class)), owner);
        var lists = jdbc.query("""
                WITH latest_active_plan AS (
                    SELECT id FROM meal_plans
                    WHERE owner_id = ? AND NOT archived
                    ORDER BY updated_at DESC, id DESC LIMIT 1
                )
                SELECT sl.id, sl.meal_plan_id, p.name, sl.total_purchase_cost, sl.total_waste_cost,
                       CASE WHEN sl.source_plan_content_version = p.content_version
                            THEN 'CURRENT' ELSE 'OUTDATED' END AS freshness,
                       sl.generated_at
                FROM shopping_lists sl
                JOIN meal_plans p ON p.id = sl.meal_plan_id
                WHERE sl.owner_id = ? AND sl.active AND NOT sl.archived
                ORDER BY CASE
                    WHEN sl.source_plan_content_version = p.content_version
                         AND sl.meal_plan_id IN (SELECT id FROM latest_active_plan) THEN 0
                    WHEN sl.source_plan_content_version = p.content_version THEN 1
                    ELSE 2 END,
                    sl.generated_at DESC, sl.id DESC
                LIMIT 1
                """, (rs, row) -> new DashboardResponse.ShoppingListCard(
                        rs.getObject("id", UUID.class), rs.getObject("meal_plan_id", UUID.class),
                        rs.getString("name"), rs.getBigDecimal("total_purchase_cost"),
                        rs.getBigDecimal("total_waste_cost"), rs.getString("freshness"),
                        rs.getObject("generated_at", java.time.OffsetDateTime.class)), owner, owner);
        return new DashboardResponse(metrics, plans.stream().findFirst().orElse(null),
                lists.stream().findFirst().orElse(null), activity.findAll(null, 0, 5).content());
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
