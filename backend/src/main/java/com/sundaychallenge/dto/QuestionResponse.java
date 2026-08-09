package com.sundaychallenge.dto;

import com.sundaychallenge.entity.Question;

/**
 * Safe Question DTO sent to students during active challenges.
 * SECURITY RULE: OMIITS correctOption and explanation prior to submission.
 */
public record QuestionResponse(
        Long id,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        Integer points,
        Integer questionOrder
) {
    public static QuestionResponse fromEntity(Question question, Integer questionOrder) {
        if (question == null) return null;
        return new QuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC(),
                question.getOptionD(),
                question.getPoints(),
                questionOrder
        );
    }
}
