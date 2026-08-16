package com.sundaychallenge.dto;

/**
 * Data Transfer Object representing the authenticated student's current position and metrics on the leaderboard.
 */
public record LeaderboardUserPositionResponse(
        int rank,
        int totalPoints,
        double averageScore,
        int bestScore,
        long completedChallenges,
        long attemptedChallenges,
        String username,
        String name,
        String profileImage
) {}
