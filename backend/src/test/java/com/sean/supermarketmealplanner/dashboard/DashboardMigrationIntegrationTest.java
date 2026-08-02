package com.sean.supermarketmealplanner.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sean.supermarketmealplanner.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class DashboardMigrationIntegrationTest extends AbstractIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void latestMigrationCreatesOrganizationAndCatalogSyncSchema() {
        var version = jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class);
        assertThat(version).isEqualTo("13");
        assertThat(column("meal_plans", "favorite")).isTrue();
        assertThat(column("meal_plans", "estimated_purchase_cost")).isTrue();
        assertThat(column("shopping_lists", "active")).isTrue();
        assertThat(column("user_preferences", "theme")).isTrue();
        assertThat(column("user_activity_events", "origin")).isTrue();
        assertThat(column("products", "last_seen_at")).isTrue();
        assertThat(column("products", "unavailable_since")).isTrue();
        assertThat(column("product_price_history", "sync_run_id")).isTrue();
        assertThat(table("catalog_sync_runs")).isTrue();
        assertThat(table("catalog_sync_errors")).isTrue();
        assertThat(table("staging_products")).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT theme FROM user_preferences WHERE user_id = "
                        + "'00000000-0000-4000-8000-000000000007'", String.class))
                .isEqualTo("SYSTEM");
    }

    @Test
    void emptyDashboardAndActivityUsePersistedProjections() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.activePlans").value(0))
                .andExpect(jsonPath("$.latestPlan").doesNotExist())
                .andExpect(jsonPath("$.selectedShoppingList").doesNotExist());
        mockMvc.perform(get("/api/v1/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    private boolean column(String table, String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                )
                """, Boolean.class, table, column));
    }

    private boolean table(String table) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_name = ?)
                """, Boolean.class, table));
    }
}
