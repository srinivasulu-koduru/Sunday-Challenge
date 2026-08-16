package com.sundaychallenge.dto;

import com.sundaychallenge.entity.Challenge;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.ChallengeStatus;
import com.sundaychallenge.entity.enums.Difficulty;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for returning full challenge metadata and schedule to admins.
 */
public record AdminChallengeResponse(
        Long id,
        String title,
        String description,
        Category category,
        Difficulty difficulty,
        Integer durationMinutes,
        Integer totalQuestions,
        Integer totalPoints,
        Boolean active,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ChallengeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminChallengeResponse fromEntity(Challenge challenge) {
        if (challenge == null) return null;
        LocalDateTime now = LocalDateTime.now();
        ChallengeStatus resolvedStatus = challenge.resolveStatus(now);

        return new AdminChallengeResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getCategory(),
                challenge.getDifficulty(),
                challenge.getDurationMinutes(),
                challenge.getTotalQuestions(),
                challenge.getTotalPoints(),
                challenge.isActive(),
                challenge.getStartTime(),
                challenge.getEndTime(),
                resolvedStatus,
                challenge.getCreatedAt(),
                challenge.getUpdatedAt()
        );
    }
}
