package com.sean.supermarketmealplanner.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.observability")
public record ObservabilityProperties(
        String environment,
        String userHashSecret,
        String version,
        String commit,
        String buildTime
) {
    public ObservabilityProperties {
        environment = blank(environment, "unknown");
        userHashSecret = userHashSecret == null ? "" : userHashSecret;
        version = blank(version, "0.11.0-dev");
        commit = blank(commit, "unknown");
        buildTime = blank(buildTime, "unknown");
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
