package com.sundaychallenge.dto;

import java.util.List;

/**
 * Data Transfer Object returning full challenge metadata along with its associated questions list for admin management.
 */
public record AdminChallengeDetailsResponse(
        AdminChallengeResponse challenge,
        List<AdminQuestionResponse> questions
) {
}
