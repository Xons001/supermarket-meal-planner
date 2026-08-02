package com.sean.supermarketmealplanner.configuration;

import com.sean.supermarketmealplanner.identity.application.AuthProperties;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class ProductionConfigurationValidator implements ApplicationRunner {
    private static final List<String> FORBIDDEN = List.of("changeme", "secret", "password", "default");
    private final Environment environment;
    private final AuthProperties auth;
    private final ObservabilityProperties observability;

    public ProductionConfigurationValidator(Environment environment, AuthProperties auth,
                                            ObservabilityProperties observability) {
        this.environment = environment;
        this.auth = auth;
        this.observability = observability;
    }

    @Override
    public void run(ApplicationArguments args) {
        var errors = new ArrayList<String>();
        validateSecret("APP_AUTH_ACCESS_TOKEN_SECRET", auth.accessTokenSecret(), errors);
        validateSecret("APP_AUTH_REFRESH_TOKEN_SECRET", auth.refreshTokenSecret(), errors);
        validateSecret("MEAL_PLAN_PREVIEW_HMAC_SECRET",
                environment.getProperty("app.meal-plans.editing.preview-hmac-secret"), errors);
        validateSecret("APP_OBSERVABILITY_USER_HASH_SECRET", observability.userHashSecret(), errors);
        validateSecret("DB_PASSWORD", environment.getProperty("spring.datasource.password"), errors);
        validateSecret("AIRFLOW_ADMIN_PASSWORD", environment.getProperty("app.catalog-sync.airflow-password"), errors);
        var secrets = Map.of(
                "access", auth.accessTokenSecret(),
                "refresh", auth.refreshTokenSecret(),
                "preview", environment.getProperty("app.meal-plans.editing.preview-hmac-secret", ""),
                "observability", observability.userHashSecret(),
                "database", environment.getProperty("spring.datasource.password", ""),
                "airflow", environment.getProperty("app.catalog-sync.airflow-password", ""));
        var configuredSecrets = secrets.values().stream().filter(value -> value != null && !value.isBlank()).toList();
        if (configuredSecrets.stream().distinct().count() != configuredSecrets.size()) {
            errors.add("production secrets must all be distinct");
        }
        if (!auth.cookie().secure()) errors.add("production cookies must use Secure=true");
        if (!Set.of("lax", "strict", "none").contains(auth.cookie().sameSite().toLowerCase(Locale.ROOT))) {
            errors.add("APP_AUTH_COOKIE_SAME_SITE must be Lax, Strict or None");
        }
        if (auth.allowedOrigins().isEmpty() || auth.allowedOrigins().stream().anyMatch(origin ->
                origin.contains("*") || !isHttps(origin))) {
            errors.add("APP_AUTH_ALLOWED_ORIGINS must contain explicit HTTPS origins");
        }
        if (auth.swaggerEnabled()) errors.add("Swagger must be disabled in production");
        if (environment.getProperty("app.demo.enabled", Boolean.class, false)) errors.add("demo account must be disabled");
        if (environment.getProperty("app.admin.enabled", Boolean.class, false)) errors.add("admin bootstrap must be disabled");
        if (environment.getProperty("app.catalog.seed-enabled", Boolean.class, false)) errors.add("catalog seed must be disabled");
        if (environment.getProperty("app.meal-templates.seed-enabled", Boolean.class, false)) errors.add("meal-template seed must be disabled");
        if ("LOCAL_JSON".equalsIgnoreCase(environment.getProperty("app.catalog-sync.provider", ""))) {
            errors.add("local catalog provider must be disabled in production");
        }
        if ("LOCAL_JSON".equalsIgnoreCase(environment.getProperty("app.nutrition-enrichment.provider", ""))) {
            errors.add("local nutrition provider must be disabled in production");
        }
        var issuer = auth.issuer();
        if (issuer == null || issuer.isBlank() || "supermarket-meal-planner".equals(issuer)) {
            errors.add("APP_AUTH_ISSUER must be explicit in production");
        }
        if (environment.getProperty("app.nutrition-enrichment.open-food-facts.enabled", Boolean.class, false)) {
            var userAgent = environment.getProperty("app.nutrition-enrichment.open-food-facts.user-agent", "");
            if (!userAgent.contains("(") || userAgent.contains("example.invalid") || userAgent.contains("local-development")) {
                errors.add("Open Food Facts requires an identifiable production User-Agent");
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Unsafe production configuration: " + String.join("; ", errors));
        }
    }

    private static void validateSecret(String name, String value, List<String> errors) {
        if (value == null || value.length() < 32) {
            errors.add(name + " must contain at least 32 characters");
            return;
        }
        var normalized = value.toLowerCase(Locale.ROOT);
        if (FORBIDDEN.stream().anyMatch(normalized::contains)) errors.add(name + " contains a forbidden placeholder");
    }

    private static boolean isHttps(String value) {
        try { return "https".equalsIgnoreCase(URI.create(value).getScheme()); }
        catch (IllegalArgumentException exception) { return false; }
    }
}
