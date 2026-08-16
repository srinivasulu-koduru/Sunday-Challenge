package com.sundaychallenge.dto;

import com.sundaychallenge.entity.AdminAccess;

import java.time.LocalDateTime;

public record AdminAccessResponse(
        Long id,
        String email,
        String name,
        String role,
        boolean active,
        boolean primaryAdmin,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminAccessResponse fromEntity(AdminAccess entity) {
        return new AdminAccessResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getName() != null && !entity.getName().isEmpty() ? entity.getName() : (entity.isPrimaryAdmin() ? "Primary Admin" : "Administrator"),
                "ADMIN",
                entity.isActive(),
                entity.isPrimaryAdmin(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
