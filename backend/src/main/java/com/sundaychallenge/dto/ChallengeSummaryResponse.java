package com.sundaychallenge.dto;

import com.sundaychallenge.entity.Challenge;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;

/**
 * Summary DTO for active challenges list.
 */
public record ChallengeSummaryResponse(
        Long id,
        String title,
        String description,
        Category category,
        Difficulty difficulty,
        Integer durationMinutes,
        Integer totalQuestions,
        Integer totalPoints
) {
    public static ChallengeSummaryResponse fromEntity(Challenge challenge) {
        if (challenge == null) return null;
        return new ChallengeSummaryResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getCategory(),
                challenge.getDifficulty(),
                challenge.getDurationMinutes(),
                challenge.getTotalQuestions(),
                challenge.getTotalPoints()
        );
    }
}
