package com.sundaychallenge.entity;

import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.ChallengeStatus;
import com.sundaychallenge.entity.enums.Difficulty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Challenge Entity representing an assessment exam/quiz with scheduling and lifecycle status.
 */
@Entity
@Table(name = "challenges")
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 2048)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private Difficulty difficulty;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ChallengeStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Challenge() {
    }

    public Challenge(String title, String description, Category category, Difficulty difficulty,
                     Integer durationMinutes, Integer totalQuestions, Integer totalPoints, boolean active) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.difficulty = difficulty;
        this.durationMinutes = durationMinutes;
        this.totalQuestions = totalQuestions;
        this.totalPoints = totalPoints;
        this.active = active;
        this.status = active ? ChallengeStatus.ACTIVE : ChallengeStatus.INACTIVE;
    }

    public Challenge(String title, String description, Category category, Difficulty difficulty,
                     Integer durationMinutes, Integer totalQuestions, Integer totalPoints, boolean active,
                     LocalDateTime startTime, LocalDateTime endTime, ChallengeStatus status) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.difficulty = difficulty;
        this.durationMinutes = durationMinutes;
        this.totalQuestions = totalQuestions;
        this.totalPoints = totalPoints;
        this.active = active;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    /**
     * Authoritative calculation of challenge lifecycle status based on active flag and server time window.
     */
    public ChallengeStatus resolveStatus(LocalDateTime now) {
        if (!active) {
            return ChallengeStatus.INACTIVE;
        }
        if (status == ChallengeStatus.DRAFT) {
            return ChallengeStatus.DRAFT;
        }
        if (startTime != null && now.isBefore(startTime)) {
            return ChallengeStatus.UPCOMING;
        }
        if (endTime != null && now.isAfter(endTime)) {
            return ChallengeStatus.COMPLETED;
        }
        return ChallengeStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public ChallengeStatus getStatus() {
        return status;
    }

    public void setStatus(ChallengeStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
