package com.sean.supermarketmealplanner.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sean.supermarketmealplanner.identity.application.IdentityException;
import com.sean.supermarketmealplanner.identity.application.InMemoryRateLimiter;
import com.sean.supermarketmealplanner.identity.application.PasswordPolicy;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserAccountEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class IdentityDomainTest {
    @Test void normalizesEmailWithoutChangingInternalCharacters() {
        assertThat(UserAccountEntity.normalizeEmail("  Person@Example.COM ")).isEqualTo("person@example.com");
    }
    @Test void passwordPolicyCountsUnicodeCodePointsAndDoesNotTrim() {
        var policy=new PasswordPolicy();
        policy.validate(" 12345678 ");
        assertThatThrownBy(()->policy.validate("😀😀😀😀😀😀😀😀😀"))
                .isInstanceOf(IdentityException.class)
                .extracting("code").isEqualTo("PASSWORD_POLICY_VIOLATION");
        policy.validate("😀😀😀😀😀😀😀😀😀😀");
    }
    @Test void rateLimiterUsesClockAndNonReversibleBuckets() {
        var clock=new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        var limiter=new InMemoryRateLimiter(clock);
        limiter.check("login","ip|mail",2,Duration.ofMinutes(1));
        limiter.check("login","ip|mail",2,Duration.ofMinutes(1));
        assertThatThrownBy(()->limiter.check("login","ip|mail",2,Duration.ofMinutes(1)))
                .isInstanceOf(IdentityException.class).extracting("code").isEqualTo("RATE_LIMIT_EXCEEDED");
        clock.now=clock.now.plusSeconds(61);
        limiter.check("login","ip|mail",2,Duration.ofMinutes(1));
    }
    private static final class MutableClock extends Clock {
        private Instant now; MutableClock(Instant now){this.now=now;}
        @Override public ZoneId getZone(){return ZoneId.of("UTC");}
        @Override public Clock withZone(ZoneId zone){return this;}
        @Override public Instant instant(){return now;}
    }
}
