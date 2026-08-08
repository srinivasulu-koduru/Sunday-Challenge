package com.sundaychallenge.dto;

/**
 * Data Transfer Object representing the health status response.
 *
 * @param status  Component operational state ("UP", "DOWN", etc.)
 * @param message Descriptive status message
 */
public record HealthResponse(String status, String message) {
}
