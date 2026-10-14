package com.ambulance.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public liveness check. Handy for hosting health checks and for confirming a deploy is up. */
@RestController
public class HealthController {

    @GetMapping({"/", "/health"})
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "ambulance-system-api");
    }
}
