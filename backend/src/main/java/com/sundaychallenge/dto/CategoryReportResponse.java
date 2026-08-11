package com.sundaychallenge.dto;

import com.sundaychallenge.entity.enums.Category;

/**
 * Data Transfer Object for category breakdown report.
 */
public record CategoryReportResponse(
        Category category,
        long totalChallenges,
        long totalAttempts,
        long completedAttempts,
        double averageScore,
        double completionRate
) {
}
