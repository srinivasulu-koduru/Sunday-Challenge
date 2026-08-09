package com.sundaychallenge.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO returned when a student starts a challenge attempt.
 */
public record StartChallengeResponse(
        Long attemptId,
        Long challengeId,
        String challengeTitle,
        Integer durationMinutes,
        LocalDateTime startedAt,
        List<QuestionResponse> questions
) {
}
