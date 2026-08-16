package com.sundaychallenge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entity representing an email account authorized to receive ADMIN privileges (admin_access table).
 * Enforces dynamic role assignment upon Google OAuth login and protects the permanent Primary Admin.
 */
@Entity
@Table(name = "admin_access")
public class AdminAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "primary_admin", nullable = false)
    private boolean primaryAdmin = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AdminAccess() {
    }

    public AdminAccess(String email, String name, boolean active, boolean primaryAdmin) {
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.name = name != null ? name.trim() : "";
        this.active = active;
        this.primaryAdmin = primaryAdmin;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : "";
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isPrimaryAdmin() {
        return primaryAdmin;
    }

    public void setPrimaryAdmin(boolean primaryAdmin) {
        this.primaryAdmin = primaryAdmin;
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
