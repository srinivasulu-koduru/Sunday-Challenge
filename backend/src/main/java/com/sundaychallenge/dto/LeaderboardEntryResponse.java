package com.sundaychallenge.dto;

/**
 * Data Transfer Object for ranking entries on the platform leaderboard.
 */
public record LeaderboardEntryResponse(
        int rank,
        Long userId,
        String username,
        String name,
        int totalPoints,
        long completedChallenges,
        double averageScore
) {
}
