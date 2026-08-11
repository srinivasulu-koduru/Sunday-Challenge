package com.sundaychallenge.dto;

/**
 * Data Transfer Object for username setup response.
 */
public record SetUsernameResponse(
        boolean success,
        String username,
        String message
) {
    public static SetUsernameResponse success(String username) {
        return new SetUsernameResponse(true, username, null);
    }

    public static SetUsernameResponse error(String message) {
        return new SetUsernameResponse(false, null, message);
    }
}
