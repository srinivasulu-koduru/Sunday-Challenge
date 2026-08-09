package com.sundaychallenge.dto;

import com.sundaychallenge.entity.enums.AttemptStatus;

import java.time.LocalDateTime;

/**
 * Authoritative evaluation DTO returned after challenge submission.
 */
public record SubmitChallengeResponse(
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
        LocalDateTime submittedAt,
        Long timeTakenSeconds
) {
}
