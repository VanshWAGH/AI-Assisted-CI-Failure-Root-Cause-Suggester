package com.rootcause.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Lightweight health/status endpoint consumed by the frontend dashboard.
 * The React {@code useServiceStatus} hook calls GET /api/v1/status to
 * determine whether the backend is reachable and updates the sidebar status dot.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Status", description = "Lightweight API liveness check")
public class StatusController {

    @GetMapping("/status")
    @Operation(summary = "API status check", description = "Returns OK when the service is up — used by the frontend to show the live status indicator")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "status",    "ok",
                "service",   "rootcause-suggester",
                "version",   "1.0.0",
                "timestamp", Instant.now().toString()
        ));
    }
}
