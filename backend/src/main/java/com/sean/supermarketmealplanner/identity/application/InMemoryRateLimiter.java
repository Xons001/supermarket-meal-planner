package com.sean.supermarketmealplanner.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRateLimiter {
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    public InMemoryRateLimiter(Clock clock) { this.clock = clock; }
    public void check(String scope, String identifier, int limit, Duration duration) {
        var key = scope + ":" + fingerprint(identifier);
        var now = clock.instant();
        var current = windows.compute(key, (ignored, previous) -> {
            if (previous == null || !previous.started().plus(duration).isAfter(now)) {
                return new Window(now, 1);
            }
            return new Window(previous.started(), previous.count() + 1);
        });
        if (current.count() > limit) {
            throw new IdentityException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Demasiados intentos. Inténtalo de nuevo más tarde");
        }
    }
    private String fingerprint(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)), 0, 12);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
    private record Window(Instant started, int count) {}
}
