package com.sundaychallenge.dto;

/**
 * DTO for returning aggregated student dashboard statistics.
 */
public record UserStatsResponse(
        Long totalChallenges,
        Long completedChallenges,
        Long pendingChallenges,
        Integer totalPoints
) {
}
