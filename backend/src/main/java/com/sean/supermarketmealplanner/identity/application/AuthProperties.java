package com.sean.supermarketmealplanner.identity.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.auth")
public record AuthProperties(
        @NotBlank @Size(min = 32) String accessTokenSecret,
        @NotBlank @Size(min = 32) String refreshTokenSecret,
        String issuer,
        Duration accessTtl,
        Duration refreshTtl,
        @Valid Cookie cookie,
        @Valid Argon2 argon2,
        List<String> allowedOrigins,
        boolean swaggerEnabled,
        @Valid RateLimits rateLimits
) {
    public AuthProperties {
        issuer = issuer == null ? "supermarket-meal-planner" : issuer;
        accessTtl = accessTtl == null ? Duration.ofMinutes(15) : accessTtl;
        refreshTtl = refreshTtl == null ? Duration.ofDays(30) : refreshTtl;
        cookie = cookie == null ? new Cookie(false, "Lax") : cookie;
        argon2 = argon2 == null ? new Argon2(16, 32, 1, 19456, 2) : argon2;
        allowedOrigins = allowedOrigins == null ? List.of("http://localhost:5173") : List.copyOf(allowedOrigins);
        rateLimits = rateLimits == null ? new RateLimits(5, 3, 30, 5) : rateLimits;
    }
    @AssertTrue(message = "access and refresh token secrets must be different")
    public boolean hasDistinctSecrets() { return !accessTokenSecret.equals(refreshTokenSecret); }
    @AssertTrue(message = "SameSite=None requires Secure=true")
    public boolean hasSafeSameSite() {
        return !"None".equalsIgnoreCase(cookie.sameSite()) || cookie.secure();
    }
    public record Cookie(boolean secure, String sameSite) {}
    public record Argon2(@Min(8) int saltLength, @Min(16) int hashLength, @Min(1) int parallelism,
                         @Min(8192) int memoryKb, @Min(1) int iterations) {}
    public record RateLimits(@Min(1) int loginPerMinute, @Min(1) int registerPerHour,
                             @Min(1) int refreshPerMinute, @Min(1) int passwordChangePerHour) {}
}
