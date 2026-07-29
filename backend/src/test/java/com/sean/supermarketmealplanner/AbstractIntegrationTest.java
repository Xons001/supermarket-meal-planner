package com.sean.supermarketmealplanner;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    private static final String PREVIEW_SECRET = randomSecret();

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRESQL.start();
    }

    @DynamicPropertySource
    static void editingProperties(DynamicPropertyRegistry registry) {
        registry.add("app.meal-plans.editing.preview-hmac-secret", () -> PREVIEW_SECRET);
    }

    private static String randomSecret() {
        var bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
