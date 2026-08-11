package com.sundaychallenge.dto;

import com.sundaychallenge.entity.Role;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for student detailed performance profile in admin portal.
 */
public record AdminStudentDetailsResponse(
        Long id,
        String username,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt,
        long totalAttempts,
        long completedAttempts,
        long expiredAttempts,
        long inProgressAttempts,
        int totalPoints,
        double averageScore,
        int bestScore,
        List<UserAttemptSummaryResponse> recentAttempts
) {
}
