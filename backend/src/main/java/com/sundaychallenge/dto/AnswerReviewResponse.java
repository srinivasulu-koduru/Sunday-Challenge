package com.sundaychallenge.dto;

/**
 * Answer Review DTO for inspecting question details, student answer, correct answer, and explanation after submission.
 */
public record AnswerReviewResponse(
        Long questionId,
        Integer questionOrder,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String selectedOption,
        String correctOption,
        boolean correct,
        Integer pointsEarned,
        String explanation
) {
}
