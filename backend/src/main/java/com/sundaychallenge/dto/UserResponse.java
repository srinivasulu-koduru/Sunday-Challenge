package com.sundaychallenge.dto;

import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;

/**
 * Data Transfer Object for returning authenticated user details.
 * Ensures security by omitting sensitive tokens and internal fields.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        String profileImage,
        Role role
) {
    /**
     * Factory method to convert a User entity into a UserResponse DTO.
     */
    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfileImage(),
                user.getRole()
        );
    }
}
