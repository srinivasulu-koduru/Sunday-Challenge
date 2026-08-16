package com.sundaychallenge.dto;

import java.util.List;

/**
 * Data Transfer Object for creating or updating a complete challenge along with its embedded question list.
 */
public record SaveChallengeWithQuestionsRequest(
        AdminChallengeRequest challenge,
        List<AdminQuestionRequest> questions
) {
}
