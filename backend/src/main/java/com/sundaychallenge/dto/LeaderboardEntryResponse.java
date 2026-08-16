package com.sundaychallenge.dto;

/**
 * Data Transfer Object for ranking entries on the platform leaderboard.
 */
public record LeaderboardEntryResponse(
        int rank,
        Long userId,
        String username,
        String name,
        String profileImage,
        long attemptedChallenges,
        long completedChallenges,
        double averageScore,
        int bestScore,
        int totalPoints,
        String participationStatus
) {
    public LeaderboardEntryResponse(int rank, Long userId, String username, String name, int totalPoints, long completedChallenges, double averageScore) {
        this(rank, userId, username, name, null, completedChallenges, completedChallenges, averageScore, totalPoints, totalPoints, completedChallenges > 0 ? "Active" : "Inactive");
    }
}
