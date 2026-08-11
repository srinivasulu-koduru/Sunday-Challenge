package com.sundaychallenge.dto;

import com.sundaychallenge.entity.Question;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for returning full question details (admin only).
 */
public record AdminQuestionResponse(
        Long id,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption,
        Integer points,
        String explanation,
        Long challengeId,
        String challengeTitle,
        LocalDateTime createdAt
) {
    public static AdminQuestionResponse fromEntity(Question question, Long challengeId, String challengeTitle) {
        if (question == null) return null;
        return new AdminQuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC(),
                question.getOptionD(),
                question.getCorrectOption(),
                question.getPoints(),
                question.getExplanation(),
                challengeId,
                challengeTitle,
                question.getCreatedAt()
        );
    }
}
