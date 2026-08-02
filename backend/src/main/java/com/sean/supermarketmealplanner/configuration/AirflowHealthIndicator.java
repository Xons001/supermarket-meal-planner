package com.sean.supermarketmealplanner.configuration;

import com.sean.supermarketmealplanner.catalogsync.application.CatalogSyncProperties;
import com.sean.supermarketmealplanner.catalogsync.infrastructure.airflow.AirflowClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

@Component("airflow")
public class AirflowHealthIndicator implements HealthIndicator {
    private static final Status DEGRADED = new Status("DEGRADED");
    private final AirflowClient client;
    private final CatalogSyncProperties properties;

    public AirflowHealthIndicator(AirflowClient client, CatalogSyncProperties properties) {
        this.client = client; this.properties = properties;
    }

    @Override
    public Health health() {
        if (!properties.enabled()) return Health.up().withDetail("integration", "disabled").build();
        return client.healthy() ? Health.up().build()
                : Health.status(DEGRADED).withDetail("integration", "temporarily unavailable").build();
    }
}
