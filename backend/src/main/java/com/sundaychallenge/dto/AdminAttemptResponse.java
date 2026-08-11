package com.sundaychallenge.dto;

import com.sundaychallenge.entity.enums.AttemptStatus;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for listing attempt logs in admin portal.
 */
public record AdminAttemptResponse(
        Long attemptId,
        Long userId,
        String studentName,
        String username,
        Long challengeId,
        String challengeTitle,
        Category category,
        Difficulty difficulty,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        AttemptStatus status,
        Integer score,
        Integer totalPoints,
        Integer correctAnswers,
        Integer wrongAnswers,
        Integer unansweredAnswers,
        Integer pointsEarned,
        Long timeTakenSeconds
) {
}
