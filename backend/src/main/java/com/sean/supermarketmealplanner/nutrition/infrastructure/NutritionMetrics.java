package com.sean.supermarketmealplanner.nutrition.infrastructure;

import io.micrometer.core.instrument.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NutritionMetrics {
    public NutritionMetrics(MeterRegistry registry, JdbcTemplate jdbc) {
        gauge(registry, jdbc, "nutrition.products.missing",
                "select count(*) from products p left join nutrition n on n.product_id=p.id where n.id is null");
        gauge(registry, jdbc, "nutrition.products.partial",
                "select count(*) from nutrition where completeness in ('PARTIAL','MINIMAL','EMPTY')");
        gauge(registry, jdbc, "nutrition.products.verified",
                "select count(*) from nutrition where verification_status in ('VERIFIED','MANUAL_OVERRIDE')");
        gauge(registry, jdbc, "nutrition.candidates.pending",
                "select count(*) from nutrition_match_candidates where status='PENDING' and expires_at>now()");
    }

    private void gauge(MeterRegistry registry, JdbcTemplate jdbc, String name, String sql) {
        Gauge.builder(name, jdbc, source -> {
            try { return source.queryForObject(sql, Long.class); }
            catch (RuntimeException ignored) { return Double.NaN; }
        }).register(registry);
    }
}
