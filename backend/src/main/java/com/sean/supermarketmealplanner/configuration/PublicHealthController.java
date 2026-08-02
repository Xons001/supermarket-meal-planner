package com.sean.supermarketmealplanner.configuration;

import java.util.Map;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicHealthController {
    private final HealthEndpoint healthEndpoint;

    public PublicHealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/healthz")
    public ResponseEntity<Map<String, String>> health() {
        var status = healthEndpoint.health().getStatus().getCode();
        var available = "UP".equals(status) || "DEGRADED".equals(status);
        return ResponseEntity.status(available ? 200 : 503)
                .body(Map.of("status", available ? "UP" : "DOWN"));
    }
}
