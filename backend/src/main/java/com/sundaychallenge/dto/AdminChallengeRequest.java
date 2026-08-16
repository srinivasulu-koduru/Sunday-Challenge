package com.sundaychallenge.dto;

import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.ChallengeStatus;
import com.sundaychallenge.entity.enums.Difficulty;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for creating or updating a challenge with scheduling parameters.
 */
public record AdminChallengeRequest(
        String title,
        String description,
        Category category,
        Difficulty difficulty,
        Integer durationMinutes,
        Boolean active,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ChallengeStatus status
) {
    public AdminChallengeRequest(String title, String description, Category category, Difficulty difficulty, Integer durationMinutes, Boolean active) {
        this(title, description, category, difficulty, durationMinutes, active, null, null, null);
    }
}
