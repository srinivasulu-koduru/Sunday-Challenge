package com.sundaychallenge.dto;

/**
 * Data Transfer Object for platform administrative statistics.
 */
public record AdminStatsResponse(
        long totalStudents,
        long totalChallenges,
        long activeChallenges,
        long totalQuestions,
        long totalAttempts,
        long completedAttempts,
        long inProgressAttempts,
        long expiredAttempts,
        int totalPointsEarned,
        double averageScore,
        double completionRate
) {
}
