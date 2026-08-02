package com.sean.supermarketmealplanner.configuration;

import org.flywaydb.core.Flyway;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("flywayReadiness")
public class FlywayReadinessHealthIndicator implements HealthIndicator {
    private final Flyway flyway;

    public FlywayReadinessHealthIndicator(Flyway flyway) { this.flyway = flyway; }

    @Override
    public Health health() {
        try {
            var info = flyway.info();
            var current = info.current();
            var pending = info.pending();
            if (current == null || pending.length > 0) {
                return Health.down().withDetail("migrationState", "pending").build();
            }
            return Health.up().withDetail("schemaVersion", current.getVersion().toString()).build();
        } catch (RuntimeException exception) {
            return Health.down().withDetail("migrationState", "unavailable").build();
        }
    }
}
