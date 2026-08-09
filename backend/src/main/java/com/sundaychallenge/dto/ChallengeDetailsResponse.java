package com.sundaychallenge.dto;

import com.sundaychallenge.entity.Challenge;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;

import java.util.List;

/**
 * Detailed DTO for challenge rules and metadata.
 */
public record ChallengeDetailsResponse(
        Long id,
        String title,
        String description,
        Category category,
        Difficulty difficulty,
        Integer durationMinutes,
        Integer totalQuestions,
        Integer totalPoints,
        List<String> rules
) {
    public static ChallengeDetailsResponse fromEntity(Challenge challenge) {
        if (challenge == null) return null;
        List<String> defaultRules = List.of(
                "Read every question carefully before selecting an answer.",
                "Timer starts as soon as you click 'Start Challenge'.",
                "You can navigate back and forth between questions before submitting.",
                "The attempt will automatically expire when time runs out.",
                "Once submitted, the attempt cannot be modified or re-submitted."
        );
        return new ChallengeDetailsResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getCategory(),
                challenge.getDifficulty(),
                challenge.getDurationMinutes(),
                challenge.getTotalQuestions(),
                challenge.getTotalPoints(),
                defaultRules
        );
    }
}
