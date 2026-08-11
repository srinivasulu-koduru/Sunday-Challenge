package com.sundaychallenge.dto;

/**
 * Data Transfer Object for creating or updating a question in the admin portal.
 */
public record AdminQuestionRequest(
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption,
        Integer points,
        String explanation,
        Long challengeId
) {
}
