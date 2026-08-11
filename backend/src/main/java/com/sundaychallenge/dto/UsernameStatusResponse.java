package com.sundaychallenge.dto;

/**
 * Data Transfer Object for username status check.
 */
public record UsernameStatusResponse(
        boolean usernameSet,
        String username
) {
    public UsernameStatusResponse(boolean usernameSet) {
        this(usernameSet, null);
    }
}
