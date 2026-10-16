package com.deplens.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "system", "DepLens – Software Supply Chain Intelligence & Cascading Blast Radius Engine",
                "version", "0.0.1-SNAPSHOT",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
