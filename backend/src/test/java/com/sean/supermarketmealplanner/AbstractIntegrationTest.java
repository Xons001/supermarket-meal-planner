package com.sean.supermarketmealplanner;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.sean.supermarketmealplanner.identity.application.AuthPrincipal;
import com.sean.supermarketmealplanner.identity.domain.UserRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import java.util.UUID;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.LegacyMockMvcSecurity.class)
public abstract class AbstractIntegrationTest {
    private static final String PREVIEW_SECRET = randomSecret();
    private static final String ACCESS_SECRET = randomSecret();
    private static final String REFRESH_SECRET = randomSecret();

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRESQL.start();
    }

    @DynamicPropertySource
    static void editingProperties(DynamicPropertyRegistry registry) {
        registry.add("app.meal-plans.editing.preview-hmac-secret", () -> PREVIEW_SECRET);
        registry.add("app.auth.access-token-secret", () -> ACCESS_SECRET);
        registry.add("app.auth.refresh-token-secret", () -> REFRESH_SECRET);
        registry.add("app.auth.argon2.memory-kb", () -> 8192);
        registry.add("app.auth.argon2.iterations", () -> 1);
        registry.add("app.auth.rate-limits.login-per-minute", () -> 100);
        registry.add("app.auth.rate-limits.register-per-hour", () -> 100);
        registry.add("app.auth.rate-limits.refresh-per-minute", () -> 100);
        registry.add("app.auth.rate-limits.password-change-per-hour", () -> 100);
    }

    private static String randomSecret() {
        var bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @TestConfiguration
    static class LegacyMockMvcSecurity {
        @Bean
        MockMvcBuilderCustomizer authenticatedLegacyRequests() {
            var principal = new AuthPrincipal(
                    UUID.fromString("00000000-0000-4000-8000-000000000007"),
                    UUID.fromString("00000000-0000-4000-8000-000000000070"),
                    UserRole.ADMIN
            );
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
            return builder -> builder.defaultRequest(get("/")
                    .with(authentication(authentication)).with(csrf()));
        }
    }
}
