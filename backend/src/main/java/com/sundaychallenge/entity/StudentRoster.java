package com.sundaychallenge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entity representing an official student record in the college roster (student_roster table).
 * Used by the T&P cell to track enrolled students and identify registered vs. unregistered portal users.
 */
@Entity
@Table(name = "student_roster")
public class StudentRoster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "roll_number", nullable = false, unique = true, length = 50)
    private String rollNumber;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = true, length = 150)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StudentRoster() {
    }

    public StudentRoster(String rollNumber, String name, String email) {
        this.rollNumber = rollNumber != null ? rollNumber.trim().toUpperCase() : null;
        this.name = name != null ? name.trim() : null;
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.rollNumber != null) {
            this.rollNumber = this.rollNumber.trim().toUpperCase();
        }
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber != null ? rollNumber.trim().toUpperCase() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
