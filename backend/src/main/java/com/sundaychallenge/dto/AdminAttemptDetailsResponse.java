package com.sundaychallenge.dto;

import com.sundaychallenge.entity.enums.AttemptStatus;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for detailed attempt view (question-by-question response breakdown) in admin portal.
 */
public record AdminAttemptDetailsResponse(
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
        Double percentage,
        Integer correctAnswers,
        Integer wrongAnswers,
        Integer unansweredAnswers,
        Integer pointsEarned,
        Long timeTakenSeconds,
        List<AnswerReviewResponse> questionDetails
) {
}
