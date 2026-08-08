package com.sundaychallenge.controller;

import com.sundaychallenge.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HealthController provides system verification endpoints to confirm
 * backend operational state and REST service connectivity.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * Endpoint to check backend service health.
     * 
     * @return ResponseEntity containing HealthResponse DTO with status "UP"
     */
    @GetMapping
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse response = new HealthResponse("UP", "Sunday Challenge backend is running");
        return ResponseEntity.ok(response);
    }
}
