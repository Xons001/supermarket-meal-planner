package com.sean.supermarketmealplanner.nutrition.application;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.nutrition-enrichment")
public record NutritionEnrichmentProperties(
        boolean enabled,
        String cron,
        String provider,
        @Min(1) @Max(500) int batchSize,
        @DecimalMin("0") @DecimalMax("100") double autoAcceptThreshold,
        @DecimalMin("0") @DecimalMax("100") double manualReviewThreshold,
        Duration cacheTtl,
        Duration rejectionCooldown,
        @Min(1) int manualRatePerHour,
        @Valid OpenFoodFacts openFoodFacts
) {
    public NutritionEnrichmentProperties {
        cron = blank(cron, "0 4 * * 1");
        provider = blank(provider, "LOCAL_JSON");
        batchSize = batchSize == 0 ? 100 : batchSize;
        autoAcceptThreshold = autoAcceptThreshold == 0 ? 95 : autoAcceptThreshold;
        manualReviewThreshold = manualReviewThreshold == 0 ? 75 : manualReviewThreshold;
        cacheTtl = cacheTtl == null ? Duration.ofDays(7) : cacheTtl;
        rejectionCooldown = rejectionCooldown == null ? Duration.ofDays(30) : rejectionCooldown;
        manualRatePerHour = manualRatePerHour == 0 ? 10 : manualRatePerHour;
        openFoodFacts = openFoodFacts == null ? OpenFoodFacts.defaults() : openFoodFacts;
    }
    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
    public record OpenFoodFacts(boolean enabled, String baseUrl, String userAgent,
            @Min(1) int timeoutSeconds, @Min(0) @Max(5) int maxRetries,
            @Min(0) long requestDelayMs) {
        public OpenFoodFacts {
            baseUrl = blank(baseUrl, "https://world.openfoodfacts.org");
            userAgent = blank(userAgent, "SupermarketMealPlanner/0.1 (local-development)");
            timeoutSeconds = timeoutSeconds == 0 ? 10 : timeoutSeconds;
            requestDelayMs = requestDelayMs == 0 ? 6500 : requestDelayMs;
        }
        static OpenFoodFacts defaults() { return new OpenFoodFacts(false, null, null, 10, 2, 6500); }
    }
}
