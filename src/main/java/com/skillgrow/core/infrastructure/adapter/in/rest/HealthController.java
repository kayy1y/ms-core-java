package com.skillgrow.core.infrastructure.adapter.in.rest;

import java.time.Clock;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Salud del servicio. APIM publica /core/v1/health → /v1/health; App Service usa /health. */
@RestController
public class HealthController {

    private final Clock clock;

    public HealthController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping({"/health", "/v1/health"})
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "ms-core-java",
                "timestampEpochMs", clock.millis());
    }
}
