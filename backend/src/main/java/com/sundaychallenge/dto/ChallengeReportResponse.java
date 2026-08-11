package com.sundaychallenge.dto;

import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;

/**
 * Data Transfer Object for challenge breakdown report.
 */
public record ChallengeReportResponse(
        Long challengeId,
        String challengeTitle,
        Category category,
        Difficulty difficulty,
        long totalAttempts,
        long completedAttempts,
        long expiredAttempts,
        double averageScore,
        int highestScore,
        double completionRate
) {
}
