package com.sean.supermarketmealplanner.configuration;

import com.sean.supermarketmealplanner.nutrition.application.NutritionEnrichmentProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

@Component("nutritionProvider")
public class NutritionProviderHealthIndicator implements HealthIndicator {
    private final NutritionEnrichmentProperties properties;
    public NutritionProviderHealthIndicator(NutritionEnrichmentProperties properties) { this.properties = properties; }

    @Override
    public Health health() {
        if (!properties.enabled()) return Health.up().withDetail("integration", "disabled").build();
        if ("OPEN_FOOD_FACTS".equalsIgnoreCase(properties.provider()) && !properties.openFoodFacts().enabled()) {
            return Health.status(new Status("DEGRADED")).withDetail("integration", "provider disabled").build();
        }
        return Health.up().withDetail("provider", properties.provider()).build();
    }
}
