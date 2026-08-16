package com.sundaychallenge.dto;

import java.util.List;

/**
 * Data Transfer Object wrapping a paginated leaderboard response,
 * platform statistics, top 3 podium entries, and current user rank position.
 */
public record LeaderboardPageResponse(
        List<LeaderboardEntryResponse> entries,
        List<LeaderboardEntryResponse> topThree,
        LeaderboardUserPositionResponse currentUserPosition,
        long totalStudents,
        long participatingStudents,
        int topScore,
        double averageScore,
        int currentPage,
        int pageSize,
        int totalPages
) {}
