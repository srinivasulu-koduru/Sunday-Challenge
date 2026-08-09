package com.sundaychallenge.dto;

import java.util.Map;

/**
 * DTO sent by student to submit a challenge attempt.
 * answers: Map of questionId -> selectedOption ("A", "B", "C", "D")
 */
public record SubmitChallengeRequest(
        Long attemptId,
        Map<Long, String> answers
) {
}
