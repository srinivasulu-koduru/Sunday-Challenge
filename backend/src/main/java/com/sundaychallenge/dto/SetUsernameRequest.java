package com.sundaychallenge.dto;

/**
 * Data Transfer Object for setting a user's platform username/roll number.
 */
public record SetUsernameRequest(
        String username
) {
}
