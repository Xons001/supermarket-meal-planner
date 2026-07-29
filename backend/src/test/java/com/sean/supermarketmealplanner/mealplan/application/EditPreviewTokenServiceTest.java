package com.sean.supermarketmealplanner.mealplan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditPreviewTokenServiceTest {
    private static final String SECRET = randomSecret();
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");

    @Test
    void issuesAndVerifiesSignedTokens() {
        var service = serviceAt(NOW);
        var payload = payload(4);

        var signed = service.issue(payload);

        assertThat(service.verify(signed.value()))
                .usingRecursiveComparison()
                .ignoringFields("expiresAtEpochSeconds")
                .isEqualTo(payload);
    }

    @Test
    void rejectsMalformedAndManipulatedTokensWithStableCodes() {
        var service = serviceAt(NOW);
        var token = service.issue(payload(4)).value();

        assertCode(() -> service.verify("not-a-token"), "EDIT_PREVIEW_TOKEN_MALFORMED", 400);
        assertCode(
                () -> service.verify(token.substring(0, token.length() - 1) + "x"),
                "EDIT_PREVIEW_TOKEN_INVALID_SIGNATURE",
                400
        );
    }

    @Test
    void treatsExpiredPreviewsAsStale() {
        var token = serviceAt(NOW).issue(payload(4)).value();
        var later = serviceAt(NOW.plus(Duration.ofMinutes(16)));

        assertCode(() -> later.verify(token), "EDIT_PREVIEW_STALE", 409);
    }

    @Test
    void failsFastWhenSecretIsTooShort() {
        var service = new EditPreviewTokenService(
                new ObjectMapper(), "short", Duration.ofMinutes(15),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        assertThatThrownBy(service::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    private static EditPreviewTokenService serviceAt(Instant instant) {
        var service = new EditPreviewTokenService(
                new ObjectMapper(), SECRET, Duration.ofMinutes(15),
                Clock.fixed(instant, ZoneOffset.UTC)
        );
        service.validate();
        return service;
    }

    private static String randomSecret() {
        var bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static EditPreviewTokenService.TokenPayload payload(long version) {
        return new EditPreviewTokenService.TokenPayload(
                "MEAL_REGENERATED",
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                UUID.fromString("20000000-0000-4000-8000-000000000001"),
                version,
                List.of(UUID.fromString("30000000-0000-4000-8000-000000000001")),
                42L,
                "result-hash",
                "PURCHASE_AWARE_SCORING",
                "BALANCED",
                "snapshot-hash",
                new MealPlanEditingDtos.MetricsSnapshot(
                        null, null, null, null, null, null, null, null,
                        null, null, null, null, null
                ),
                "purchase-aware-beam-v1",
                0L
        );
    }

    private static void assertCode(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable invocation,
            String code,
            int status
    ) {
        assertThatThrownBy(invocation)
                .isInstanceOf(MealPlanEditingException.class)
                .satisfies(error -> {
                    var exception = (MealPlanEditingException) error;
                    assertThat(exception.errorCode()).isEqualTo(code);
                    assertThat(exception.status()).isEqualTo(status);
                });
    }
}
