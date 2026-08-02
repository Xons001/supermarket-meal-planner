package com.sean.supermarketmealplanner.catalogsync.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.catalog-sync")
public record CatalogSyncProperties(
        boolean enabled,
        @NotBlank String provider,
        @NotBlank String airflowBaseUrl,
        String airflowPublicUrl,
        @NotBlank String airflowUsername,
        @NotBlank String airflowPassword,
        @NotBlank String fullSchedule,
        @NotBlank String priceSchedule,
        @Min(1) int manualRatePerHour,
        @Min(1) int stagingRetentionDays
) {}
