package com.sundaychallenge.dto;

import com.sundaychallenge.entity.enums.AttemptStatus;

import java.time.LocalDateTime;

/**
 * Result DTO for viewing challenge attempt outcome.
 */
public record ChallengeResultResponse(
        Long attemptId,
        Long challengeId,
        String challengeTitle,
        Integer score,
        Integer totalPoints,
        Double percentage,
        Integer correctAnswers,
        Integer wrongAnswers,
        Integer unansweredAnswers,
        Integer pointsEarned,
        AttemptStatus status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        Long timeTakenSeconds
) {
}
