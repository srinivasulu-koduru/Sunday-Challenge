package com.sundaychallenge.dto;

import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserResponseTest {

    @Test
    void fromEntity_ShouldMapAllFieldsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User("google-12345", "Test Student", "student@gmail.com", "http://image.url", Role.STUDENT);
        user.setId(42L);
        user.setCreatedAt(now);

        UserResponse dto = UserResponse.fromEntity(user);

        assertNotNull(dto);
        assertEquals(42L, dto.id());
        assertEquals("Test Student", dto.name());
        assertEquals("student@gmail.com", dto.email());
        assertEquals("http://image.url", dto.profileImage());
        assertEquals(Role.STUDENT, dto.role());
        assertEquals(now, dto.createdAt());
    }
}
