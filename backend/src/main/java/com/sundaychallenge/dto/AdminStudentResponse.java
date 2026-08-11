package com.sundaychallenge.dto;

import com.sundaychallenge.entity.Role;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for admin student directory list.
 */
public record AdminStudentResponse(
        Long id,
        String username,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt,
        long totalAttempts,
        long completedChallenges,
        int totalPoints,
        double averageScore
) {
}
