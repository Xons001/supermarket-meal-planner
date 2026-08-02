package com.sean.supermarketmealplanner.configuration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sean.supermarketmealplanner.identity.application.AuthProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigurationValidatorTest {
    @Test
    void acceptsAnExplicitSafeProductionConfiguration() {
        var environment = environment("db-value-abcdefghijklmnopqrstuvwxyz123", "air-value-abcdefghijklmnopqrstuvwxyz12");
        var validator = new ProductionConfigurationValidator(environment, auth(true, false),
                new ObservabilityProperties("production", "obs-value-abcdefghijklmnopqrstuvwxyz12", "1.0.0", "abc", "now"));
        assertThatCode(() -> validator.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }

    @Test
    void rejectsPlaceholdersInsecureCookiesAndRepeatedSecrets() {
        var repeated = "repeated-value-abcdefghijklmnopqrstuvwxyz";
        var environment = environment(repeated, repeated);
        var validator = new ProductionConfigurationValidator(environment, auth(false, true),
                new ObservabilityProperties("production", repeated, "1.0.0", "abc", "now"));
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production secrets must all be distinct")
                .hasMessageContaining("Secure=true")
                .hasMessageContaining("Swagger must be disabled");
    }

    private static MockEnvironment environment(String database, String airflow) {
        return new MockEnvironment()
                .withProperty("app.meal-plans.editing.preview-hmac-secret", "preview-value-abcdefghijklmnopqrstuvwxyz")
                .withProperty("spring.datasource.password", database)
                .withProperty("app.catalog-sync.airflow-password", airflow)
                .withProperty("app.catalog-sync.provider", "DISABLED")
                .withProperty("app.nutrition-enrichment.provider", "DISABLED");
    }

    private static AuthProperties auth(boolean secure, boolean swagger) {
        return new AuthProperties(
                "access-value-abcdefghijklmnopqrstuvwxyz",
                "refresh-value-abcdefghijklmnopqrstuvwxyz",
                "https://meal-planner.example.com",
                Duration.ofMinutes(15), Duration.ofDays(30),
                new AuthProperties.Cookie(secure, "Lax"),
                new AuthProperties.Argon2(16, 32, 1, 19456, 2),
                List.of("https://meal-planner.example.com"), swagger,
                new AuthProperties.RateLimits(5, 3, 30, 5));
    }
}
