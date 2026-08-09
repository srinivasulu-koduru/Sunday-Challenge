package com.sundaychallenge.dto;

import com.sundaychallenge.entity.enums.AttemptStatus;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;

import java.time.LocalDateTime;

/**
 * DTO for rendering past challenge attempts in My Results page & Student Dashboard.
 */
public record UserAttemptSummaryResponse(
        Long attemptId,
        Long challengeId,
        String challengeTitle,
        Category category,
        Difficulty difficulty,
        Integer score,
        Integer totalPoints,
        Double percentage,
        Integer pointsEarned,
        AttemptStatus status,
        LocalDateTime submittedAt
) {
}
