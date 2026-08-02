package com.sean.supermarketmealplanner.configuration;

import com.sean.supermarketmealplanner.identity.application.AuthPrincipal;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestObservabilityFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_ATTRIBUTE = "smp.requestId";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");
    private static final org.slf4j.Logger log = LoggerFactory.getLogger("http-access");
    private final ObservabilityProperties properties;
    private final Tracer tracer;

    public RequestObservabilityFilter(ObservabilityProperties properties, Tracer tracer) {
        this.properties = properties;
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var supplied = request.getHeader("X-Request-ID");
        var requestId = supplied != null && SAFE_ID.matcher(supplied).matches() ? supplied : UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader("X-Request-ID", requestId);
        long started = System.nanoTime();
        var currentSpan = tracer.currentSpan();
        var ownsSpan = currentSpan == null;
        var span = ownsSpan ? tracer.nextSpan().name("http " + request.getMethod()).start() : currentSpan;
        var traceId = span.context().traceId();
        var spanId = span.context().spanId();
        try (var ignoredScope = tracer.withSpan(span);
             var ignoredRequest = MDC.putCloseable("requestId", requestId);
             var ignoredEnvironment = MDC.putCloseable("environment", properties.environment());
             var ignoredVersion = MDC.putCloseable("version", properties.version());
             var ignoredTrace = MDC.putCloseable("traceId", traceId);
             var ignoredSpan = MDC.putCloseable("spanId", spanId)) {
            chain.doFilter(request, response);
        } finally {
            var principal = request.getAttribute(AuthPrincipal.class.getName());
            var pathTemplate = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            try (var ignoredRequest = MDC.putCloseable("requestId", requestId);
                 var ignoredEnvironment = MDC.putCloseable("environment", properties.environment());
                 var ignoredVersion = MDC.putCloseable("version", properties.version());
                 var ignoredTrace = MDC.putCloseable("traceId", traceId);
                 var ignoredSpan = MDC.putCloseable("spanId", spanId);
                 var ignoredUser = MDC.putCloseable("userIdHash", hashUser(principal));
                 var ignoredMethod = MDC.putCloseable("method", request.getMethod());
                 var ignoredPath = MDC.putCloseable("pathTemplate", pathTemplate == null ? request.getRequestURI() : pathTemplate.toString());
                 var ignoredStatus = MDC.putCloseable("status", Integer.toString(response.getStatus()));
                 var ignoredDuration = MDC.putCloseable("durationMs", Long.toString(durationMs))) {
                log.info("http_request_completed");
            }
            MDC.clear();
            if (ownsSpan) span.end();
        }
    }

    private String hashUser(Object principal) {
        if (!(principal instanceof AuthPrincipal auth) || properties.userHashSecret().isBlank()) return "anonymous";
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.userHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(auth.userId().toString().getBytes(StandardCharsets.UTF_8)), 0, 12);
        } catch (Exception exception) {
            return "unavailable";
        }
    }
}
