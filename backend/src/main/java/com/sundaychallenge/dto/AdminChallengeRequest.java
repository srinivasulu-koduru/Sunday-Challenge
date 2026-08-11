package com.sundaychallenge.dto;

import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;

/**
 * Data Transfer Object for creating or updating a challenge.
 */
public record AdminChallengeRequest(
        String title,
        String description,
        Category category,
        Difficulty difficulty,
        Integer durationMinutes,
        Boolean active
) {
}
