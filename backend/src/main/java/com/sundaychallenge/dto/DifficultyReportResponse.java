package com.sundaychallenge.dto;

import com.sundaychallenge.entity.enums.Difficulty;

/**
 * Data Transfer Object for difficulty breakdown report.
 */
public record DifficultyReportResponse(
        Difficulty difficulty,
        long totalChallenges,
        long totalAttempts,
        long completedAttempts,
        double averageScore,
        double completionRate
) {
}
