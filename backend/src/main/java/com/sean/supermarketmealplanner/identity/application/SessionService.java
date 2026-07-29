package com.sean.supermarketmealplanner.identity.application;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sean.supermarketmealplanner.identity.domain.UserStatus;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.RefreshTokenSessionEntity;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.RefreshTokenSessionRepository;
import com.sean.supermarketmealplanner.identity.infrastructure.persistence.UserAccountEntity;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
    private final AuthProperties properties;
    private final RefreshTokenSessionRepository sessions;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final JwtEncoder encoder;
    private final IdentityAuditLogger audit;

    public SessionService(AuthProperties properties, RefreshTokenSessionRepository sessions, Clock clock,
                          IdentityAuditLogger audit) {
        this.properties = properties; this.sessions = sessions; this.clock = clock;
        this.audit = audit;
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                properties.accessTokenSecret().getBytes(StandardCharsets.UTF_8)));
    }

    @Transactional
    public SessionTokens create(UserAccountEntity user, String userAgent) {
        return create(user, UUID.randomUUID(), userAgent);
    }

    private SessionTokens create(UserAccountEntity user, UUID family, String userAgent) {
        var now = OffsetDateTime.now(clock);
        var raw = new byte[32]; random.nextBytes(raw);
        var refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        var id = UUID.randomUUID();
        sessions.save(new RefreshTokenSessionEntity(id, hash(refresh), family, user,
                now.plus(properties.refreshTtl()), userAgent, now));
        return new SessionTokens(access(user, id, now), refresh);
    }

    @Transactional(noRollbackFor = IdentityException.class)
    public SessionTokens rotate(String rawToken, String userAgent) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IdentityException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "Sesión no válida");
        }
        var now = OffsetDateTime.now(clock);
        var old = sessions.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new IdentityException(HttpStatus.UNAUTHORIZED,
                        "REFRESH_TOKEN_INVALID", "Sesión no válida"));
        if (old.getReplacedBy() != null) {
            sessions.revokeFamily(old.getFamilyId(), now);
            audit.success("refresh_token_reused",old.getUser().getId());
            throw new IdentityException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED",
                    "Se ha detectado la reutilización de una sesión y se ha revocado su familia");
        }
        if (!old.isUsableAt(now) || old.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new IdentityException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "La sesión ha caducado");
        }
        var raw = new byte[32]; random.nextBytes(raw);
        var refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        var replacementId = UUID.randomUUID();
        sessions.save(new RefreshTokenSessionEntity(replacementId, hash(refresh), old.getFamilyId(),
                old.getUser(), now.plus(properties.refreshTtl()), userAgent, now));
        old.rotateTo(replacementId, now);
        sessions.save(old);
        return new SessionTokens(access(old.getUser(), replacementId, now), refresh);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessions.findByTokenHash(hash(rawToken)).ifPresent(session -> {
            session.revoke(OffsetDateTime.now(clock)); sessions.save(session);
        });
    }

    @Transactional
    public void revokeAll(UUID userId) {
        sessions.revokeAllByUserId(userId, OffsetDateTime.now(clock));
    }

    public String hash(String raw) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.refreshTokenSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    private String access(UserAccountEntity user, UUID sessionId, OffsetDateTime now) {
        var claims = JwtClaimsSet.builder().issuer(properties.issuer())
                .subject(user.getId().toString()).issuedAt(now.toInstant())
                .expiresAt(now.plus(properties.accessTtl()).toInstant())
                .claim("role", user.getRole().name()).claim("sid", sessionId.toString()).build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
