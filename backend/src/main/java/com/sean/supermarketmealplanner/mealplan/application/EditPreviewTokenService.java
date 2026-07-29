package com.sean.supermarketmealplanner.mealplan.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EditPreviewTokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final ObjectMapper objectMapper;
    private final String secret;
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public EditPreviewTokenService(
            ObjectMapper objectMapper,
            @Value("${app.meal-plans.editing.preview-hmac-secret}") String secret,
            @Value("${app.meal-plans.editing.preview-ttl:PT15M}") Duration ttl
    ) {
        this(objectMapper, secret, ttl, Clock.systemUTC());
    }

    EditPreviewTokenService(ObjectMapper objectMapper, String secret, Duration ttl, Clock clock) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.ttl = ttl;
        this.clock = clock;
    }

    @PostConstruct
    void validate() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "MEAL_PLAN_PREVIEW_HMAC_SECRET must contain at least 32 bytes"
            );
        }
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("Meal-plan edit preview TTL must be positive");
        }
    }

    public SignedToken issue(TokenPayload value) {
        var expiresAt = clock.instant().plus(ttl);
        var payload = new TokenPayload(
                value.operation(),
                value.planId(),
                value.ownerId(),
                value.targetId(),
                value.editVersion(),
                value.templateIds(),
                value.seed(),
                value.resultHash(),
                value.strategy(),
                value.preset(),
                value.snapshotHash(),
                value.metrics(),
                value.algorithm(),
                expiresAt.getEpochSecond()
        );
        try {
            var encoded = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            var signature = ENCODER.encodeToString(sign(encoded));
            return new SignedToken(encoded + "." + signature, expiresAt);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create edit preview token", exception);
        }
    }

    public TokenPayload verify(String token) {
        if (token == null || token.isBlank()) {
            throw malformed();
        }
        var parts = token.split("\\.", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw malformed();
        }
        try {
            var expected = sign(parts[0]);
            var supplied = DECODER.decode(parts[1]);
            if (!ENCODER.encodeToString(supplied).equals(parts[1])
                    || !MessageDigest.isEqual(expected, supplied)) {
                throw new MealPlanEditingException(
                        "The edit preview token signature is invalid",
                        "EDIT_PREVIEW_TOKEN_INVALID_SIGNATURE",
                        400
                );
            }
            var payload = objectMapper.readValue(DECODER.decode(parts[0]), TokenPayload.class);
            if (!clock.instant().isBefore(Instant.ofEpochSecond(payload.expiresAtEpochSeconds()))) {
                throw new MealPlanEditingException(
                        "The edit preview has expired",
                        "EDIT_PREVIEW_STALE",
                        409
                );
            }
            return payload;
        } catch (MealPlanEditingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw malformed();
        }
    }

    private byte[] sign(String encodedPayload) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
    }

    private MealPlanEditingException malformed() {
        return new MealPlanEditingException(
                "The edit preview token is malformed",
                "EDIT_PREVIEW_TOKEN_MALFORMED",
                400
        );
    }

    public record TokenPayload(
            String operation,
            UUID planId,
            UUID ownerId,
            UUID targetId,
            long editVersion,
            List<UUID> templateIds,
            long seed,
            String resultHash,
            String strategy,
            String preset,
            String snapshotHash,
            MealPlanEditingDtos.MetricsSnapshot metrics,
            String algorithm,
            long expiresAtEpochSeconds
    ) {
        public TokenPayload(String operation, UUID planId, UUID targetId, long editVersion,
                            List<UUID> templateIds, long seed, String resultHash, String strategy,
                            String preset, String snapshotHash, MealPlanEditingDtos.MetricsSnapshot metrics,
                            String algorithm, long expiresAtEpochSeconds) {
            this(operation, planId, null, targetId, editVersion, templateIds, seed, resultHash,
                    strategy, preset, snapshotHash, metrics, algorithm, expiresAtEpochSeconds);
        }
    }

    public record SignedToken(String value, Instant expiresAt) {
    }
}
